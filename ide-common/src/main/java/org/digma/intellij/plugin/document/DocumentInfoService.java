package org.digma.intellij.plugin.document;

import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiFile;
import org.digma.intellij.plugin.analytics.AnalyticsProvider;
import org.digma.intellij.plugin.model.MethodInfo;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DocumentInfoService {

    private final Project project;
    private final AnalyticsProvider analyticsProvider;

    //temp
    private String environment = "UNSET_ENV";

    private final Map<PsiFile, DocumentInfoContainer> documents = Collections.synchronizedMap(new HashMap<>());

    public DocumentInfoService(Project project) {
        this.project = project;
        analyticsProvider = project.getService(AnalyticsProvider.class);
    }



    public void notifyDocumentInfoChanged(PsiFile psiFile) {
        DocumentInfoChanged publisher = project.getMessageBus().syncPublisher(DocumentInfoChanged.DOCUMENT_INFO_CHANGE_TOPIC);
        publisher.documentInfoChanged(psiFile);
    }

    public void addMethodInfos(PsiFile docPsiFile, List<MethodInfo> methodInfos) {

        DocumentInfoContainer documentInfo =  documents.computeIfAbsent(docPsiFile, psiFile -> new DocumentInfoContainer(psiFile));

        documentInfo.addMethods(methodInfos,analyticsProvider,environment);

        notifyDocumentInfoChanged(docPsiFile);
    }


    public DocumentInfoContainer getDocumentInfo(PsiFile psiFile) {
        return documents.get(psiFile);
    }
}