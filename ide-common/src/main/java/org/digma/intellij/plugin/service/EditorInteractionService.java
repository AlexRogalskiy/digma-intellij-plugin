package org.digma.intellij.plugin.service;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.analytics.BackendConnectionUtil;
import org.digma.intellij.plugin.common.Backgroundable;
import org.digma.intellij.plugin.document.DocumentInfoContainer;
import org.digma.intellij.plugin.document.DocumentInfoService;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.ui.CaretContextService;
import org.digma.intellij.plugin.ui.MainToolWindowCardsController;
import org.digma.intellij.plugin.ui.service.ErrorsViewService;
import org.digma.intellij.plugin.ui.service.InsightsViewService;

import java.util.ArrayList;

/**
 * A service to implement the interactions between listeners and UI components.
 * All work should be done on EDT.
 */
public class EditorInteractionService implements CaretContextService, Disposable {

    private final Logger logger = Logger.getInstance(EditorInteractionService.class);

    private final Project project;

    private final InsightsViewService insightsViewService;
    private final ErrorsViewService errorsViewService;
    private final DocumentInfoService documentInfoService;

    /*
    EditorInteractionService has many dependencies. but EditorInteractionService should not be a dependency of too many
    other services because it will increase the possibility for cyclic dependencies. in most cases its better to use
    the getInstance only where necessary.
     */
    public EditorInteractionService(Project project) {
        this.project = project;
        insightsViewService = project.getService(InsightsViewService.class);
        errorsViewService = project.getService(ErrorsViewService.class);
        documentInfoService = project.getService(DocumentInfoService.class);
    }

    public static CaretContextService getInstance(Project project) {
        return project.getService(CaretContextService.class);
    }

    @Override
    public void contextChanged(MethodUnderCaret methodUnderCaret) {

        Log.log(logger::debug, "contextChanged called for '{}'", methodUnderCaret);

        if (project.isDisposed()) {
            Log.log(logger::debug, "project is disposed in contextChanged for '{}'", methodUnderCaret.getId());
            return;
        }

        //There is no need to execute the contextChanged flow if there is no connection to the backend.
        // testConnectionToBackend will trigger environment refresh that will either refresh the environments
        // or mark the connection as lost.
        //todo: one side effect here is that if connection lost and regained the context will still be the last one that
        // succeeded and that's what will be shown to user until another context change.
        if (!BackendConnectionUtil.getInstance(project).testConnectionToBackend()) {
            Log.log(logger::debug, "No connection to backend, not executing contextChanged for '{}'", methodUnderCaret.getId());
            MainToolWindowCardsController.getInstance(project).showMainPanel();
            return;
        }


        Backgroundable.ensureBackground(project, "Digma: Context change", () -> {
            Log.log(logger::debug, "Executing contextChanged in background for {}", methodUnderCaret.getId());
            var stopWatch = StopWatch.createStarted();
            try {
                contextChangedImpl(methodUnderCaret);
            } finally {
                stopWatch.stop();
                Log.log(logger::debug, "contextChangedImpl time took {} milliseconds", stopWatch.getTime(java.util.concurrent.TimeUnit.MILLISECONDS));
            }
        });
    }


    private void contextChangedImpl(MethodUnderCaret methodUnderCaret) {

        /*
        This method assumes that a MethodInfo should be found in DocumentInfoService. it is called only in smart mode
        and code object discovery should be complete.
         */

        Log.log(logger::debug, "contextChangedImpl invoked for '{}'", methodUnderCaret);

        if (!methodUnderCaret.isSupportedFile()) {
            Log.log(logger::debug, "methodUnderCaret is non supported file {}. ", methodUnderCaret);
            MainToolWindowCardsController.getInstance(project).showNonSupported();
            contextEmptyNonSupportedFile(methodUnderCaret.getFileUri());
            return;
        }
        if (methodUnderCaret.getFileUri().isBlank()) {
            Log.log(logger::debug, "No fileUri in methodUnderCaret,clearing context {}. ", methodUnderCaret);
            MainToolWindowCardsController.getInstance(project).showNoFile();
            contextEmpty();
            return;
        }

        String className = methodUnderCaret.getClassName();

        MainToolWindowCardsController.getInstance(project).showMainPanel();

        if (methodUnderCaret.getId().isBlank()) { // caret not under method
            Log.log(logger::debug, "No id in methodUnderCaret, Showing document preview  {}. ", methodUnderCaret);
            //if no id then try to show a preview for the document
            /*if(!className.isEmpty()){ // caret under class }*/
            DocumentInfoContainer documentInfoContainer = documentInfoService.getDocumentInfo(methodUnderCaret);
            if (documentInfoContainer == null) {
                Log.log(logger::debug, "Could not find document info for {}, Showing empty preview.", methodUnderCaret);
            } else {
                Log.log(logger::debug, "Found document info for {}. document: {}", methodUnderCaret, documentInfoContainer.getPsiFile());
            }
            insightsViewService.showDocumentPreviewList(documentInfoContainer, methodUnderCaret.getFileUri());
            errorsViewService.showDocumentPreviewList(documentInfoContainer, methodUnderCaret.getFileUri());
        }else {
            MethodInfo methodInfo = documentInfoService.getMethodInfo(methodUnderCaret);
            if (methodInfo == null) {
                Log.log(logger::warn, "Could not find MethodInfo for MethodUnderCaret {}. ", methodUnderCaret);
                //this happens when we don't have method info for a real method, usually when a class doesn't have
                //code objects found during discovery, it can be synthetic or auto-generated methods.
                //pass a dummy method info just to populate the view,the view is aware and will not try to query for insights.
                var dummyMethodInfo = new MethodInfo(methodUnderCaret.getId(), methodUnderCaret.getName(), className, "",
                        methodUnderCaret.getFileUri(), 0, new ArrayList<>());
                Log.log(logger::warn, "Using dummy MethodInfo for to update views {}. ", dummyMethodInfo);
                insightsViewService.contextChangeNoMethodInfo(dummyMethodInfo);
                errorsViewService.contextChangeNoMethodInfo(dummyMethodInfo);
            } else {
                Log.log(logger::debug, "Context changed to {}. ", methodInfo);
                DocumentInfoContainer documentInfo = documentInfoService.getDocumentInfo(methodUnderCaret);
                boolean methodHasNewInsights = documentInfo.loadInsightsForMethod(methodInfo.getId()); // might be long call since going to the backend
                insightsViewService.updateInsightsModel(methodInfo);
                errorsViewService.updateErrorsModel(methodInfo);
            }
        }
    }

    public void contextEmptyNonSupportedFile(String fileUri) {
        Log.log(logger::debug, "contextEmptyNonSupportedFile called");
        insightsViewService.emptyNonSupportedFile(fileUri);
        errorsViewService.emptyNonSupportedFile(fileUri);
    }

    /**
     * contextEmpty should be called only when there is no file opened in the editor and not in
     * any other case.
     */
    @Override
    public void contextEmpty() {
        Log.log(logger::debug, "contextEmpty called");
        insightsViewService.empty();
        errorsViewService.empty();
    }

    @Override
    public void dispose() {
        Log.log(logger::debug, "disposing..");
    }

}