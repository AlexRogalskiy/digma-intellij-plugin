package org.digma.intellij.plugin.document;

import com.intellij.psi.PsiFile;
import com.intellij.util.messages.Topic;

//a topic to listen when a document is analyzed and has new code objects
public interface DocumentCodeObjectsChanged {

    Topic<DocumentCodeObjectsChanged> DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC = Topic.create("DOCUMENT_CODE_OBJECTS_CHANGED_TOPIC", DocumentCodeObjectsChanged.class);

    void documentCodeObjectsChanged(PsiFile psiFile);
}