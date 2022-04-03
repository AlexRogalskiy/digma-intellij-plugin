package org.digma.intellij.plugin.listener;

import com.intellij.openapi.Disposable;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.event.*;
import com.intellij.openapi.fileEditor.*;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Disposer;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.*;
import com.intellij.util.messages.MessageBusConnection;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.service.EditorInteractionService;
import org.jetbrains.annotations.NotNull;

import java.util.*;


/**
 * Listens to FileEditorManager events and CaretEvents and updates view respectively.
 */
public class EditorListener implements FileEditorManagerListener {

    private static final Logger LOGGER = Logger.getInstance(EditorListener.class);

    private boolean active = false;
    private final Project project;
    private final EditorInteractionService editorInteractionService;
    private MessageBusConnection messageBusConnection;

    /**
     * EditorListener registers CaretListeners on editors, those listeners need to be removed from
     * the editor on editor close and on ide shutdown. the recommended jetbrains way is to register
     * listeners with a parent disposable. EditorListener registers a parent disposable for each
     * installed listener, on fileClosed the parent disposable should be disposed so that the listener
     * is removed, disposables keeps those parent disposables, they are disposed on fileClosed and stop.
     */
    private final Map<VirtualFile, Disposable> disposables = new HashMap<>();


    public EditorListener(Project project, EditorInteractionService editorInteractionService) {
        this.project = project;
        this.editorInteractionService = editorInteractionService;
    }


    @Override
    public void fileOpened(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile file) {
        Log.log(LOGGER::debug, "fileOpened: file:{}", file);
    }


    @Override
    public void fileClosed(@NotNull FileEditorManager fileEditorManager, @NotNull VirtualFile file) {
        Log.log(LOGGER::debug, "fileClosed: file:{}", file);
        if (disposables.containsKey(file)) {
            Log.log(LOGGER::debug, "fileClosed: disposing disposable for file:{}", file);
            Disposer.dispose(disposables.remove(file));
        }
    }


    @Override
    public void selectionChanged(@NotNull FileEditorManagerEvent editorManagerEvent) {
        FileEditorManager fileEditorManager = editorManagerEvent.getManager();
        Log.log(LOGGER::debug, "selectionChanged: editor:{}, newFile:{}, oldFile:{}", fileEditorManager.getSelectedEditor(),
                editorManagerEvent.getNewFile(), editorManagerEvent.getOldFile());

        var newFile = editorManagerEvent.getNewFile();

        var editor = fileEditorManager.getSelectedTextEditor();
        if (editor != null && !disposables.containsKey(newFile)) {
            addCaretListener(editor, newFile);
        }

        if (editor != null) {
            Log.log(LOGGER::debug, "selectionChanged: updating with file:{}", newFile);
            updateCurrentElement(editor.getCaretModel().getOffset(), newFile);
        }
    }


    //todo: don't install listener on non code files. check language and clean content when they are selected.
    private void addCaretListener(Editor editor, VirtualFile newFile) {
        if (editor == null || editor.isDisposed()) {
            Log.log(LOGGER::debug, "not installing listener for {} because its is either null or disposed", editor);
            return;
        }

        Disposable parentDisposable = Disposer.newDisposable();
        disposables.put(newFile, parentDisposable);
        Log.log(LOGGER::debug, "adding caret listener for file:{}", newFile);
        editor.getCaretModel().addCaretListener(new CaretListener() {
            @Override
            public void caretPositionChanged(@NotNull CaretEvent caretEvent) {
                Log.log(LOGGER::debug, "caretPositionChanged for editor:{}", caretEvent.getEditor());
                if (caretEvent.getCaret() != null) {
                    int caretOffset = caretEvent.getCaret().getOffset();
                    updateCurrentElement(caretOffset, newFile);
                }
            }
        }, parentDisposable);

    }


    private void updateCurrentElement(int caretOffset, @NotNull VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        if (psiFile != null) {
            PsiElement psiElement = psiFile.findElementAt(caretOffset);
            if (psiElement != null) {
                Log.log(LOGGER::debug, "got psi element {}", psiElement);
                editorInteractionService.updateViewContent(psiElement);
            }
        }
    }


    public void start() {
        if (active) {
            Log.log(LOGGER::error, "trying to start an already active listener");
            return;
        }
        Log.log(LOGGER::debug, "starting");
        messageBusConnection = project.getMessageBus().connect(editorInteractionService);
        messageBusConnection.subscribe(FileEditorManagerListener.FILE_EDITOR_MANAGER, this);
        active = true;
    }


    public void stop() {
        if (active) {
            Log.log(LOGGER::debug, "stopping...");
            messageBusConnection.disconnect();
            disposables.values().forEach(Disposer::dispose);
            active = false;
        }
    }
}
