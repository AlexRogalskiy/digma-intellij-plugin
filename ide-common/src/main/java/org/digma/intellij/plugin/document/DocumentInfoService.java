package org.digma.intellij.plugin.document;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.analytics.Environment;
import org.digma.intellij.plugin.analytics.EnvironmentChanged;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.digma.intellij.plugin.model.rest.summary.MethodCodeObjectSummary;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public class DocumentInfoService implements EnvironmentChanged {

    private final Project project;
    private final AnalyticsProvider analyticsProvider;
    private final Environment environment;


    private final Map<PsiFile, DocumentInfoContainer> documents = Collections.synchronizedMap(new HashMap<>());

    public DocumentInfoService(Project project) {
        this.project = project;
        analyticsProvider = project.getService(AnalyticsProvider.class);
        environment = project.getService(Environment.class);

        project.getMessageBus().connect().subscribe(EnvironmentChanged.ENVIRONMENT_CHANGED_TOPIC,this);
    }


    @Override
    public void environmentChanged(String newEnv) {
        documents.clear();
    }

    public void notifyDocumentInfoChanged(PsiFile psiFile) {
        DocumentInfoChanged publisher = project.getMessageBus().syncPublisher(DocumentInfoChanged.DOCUMENT_INFO_CHANGED_TOPIC);
        publisher.documentInfoChanged(psiFile);
    }


    //called after a document is analyzed for code objects
    public void addCodeObjects(PsiFile psiFile, DocumentInfo documentInfo) {
        DocumentInfoContainer documentInfoContainer = documents.computeIfAbsent(psiFile, DocumentInfoContainer::new);
        documentInfoContainer.update(documentInfo, analyticsProvider, environment.getCurrent());
        notifyDocumentInfoChanged(psiFile);
    }


    public DocumentInfoContainer getDocumentInfo(PsiFile psiFile) {
        return documents.get(psiFile);
    }


    @Nullable
    public MethodCodeObjectSummary getMethodSummaries(@NotNull MethodUnderCaret methodUnderCaret) {
        String id = methodUnderCaret.getId();
        String fileUri = methodUnderCaret.getFileUri();
        PsiFile psiFile = PsiUtils.uriToPsiFile(fileUri, project);
        DocumentInfoContainer documentInfoContainer = documents.get(psiFile);
        return documentInfoContainer == null ? null : documentInfoContainer.getMethodSummaries(id);
    }


    @Nullable
    public MethodInfo getMethodInfo(MethodUnderCaret elementUnderCaret) {
        PsiFile psiFile = PsiUtils.uriToPsiFile(elementUnderCaret.getFileUri(), project);
        DocumentInfoContainer documentInfoContainer = documents.get(psiFile);
        if (documentInfoContainer != null) {
            return documentInfoContainer.getMethodInfo(elementUnderCaret.getId());
        }
        return null;
    }
}
