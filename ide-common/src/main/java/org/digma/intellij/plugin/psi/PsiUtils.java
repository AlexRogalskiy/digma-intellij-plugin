package org.digma.intellij.plugin.psi;

import com.intellij.openapi.application.ReadAction;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileManager;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiManager;
import org.digma.intellij.plugin.model.discovery.MethodUnderCaret;
import org.jetbrains.annotations.Nullable;

/**
 * Query and navigate PSI elements.
 * This is the only class dealing with language specific services.
 */
public class PsiUtils {

    private static final Logger LOGGER = Logger.getInstance(PsiUtils.class);

    @Nullable
    public static MethodUnderCaret detectMethodUnderCaret(Project project, LanguageService languageService, int caretOffset, VirtualFile file) {
        PsiFile psiFile = PsiManager.getInstance(project).findFile(file);
        return languageService.detectMethodUnderCaret(project, psiFile, caretOffset);
    }


    @Nullable
    public static PsiFile uriToPsiFile(String uri, Project project) {
        return ReadAction.compute(() -> {
            VirtualFile virtualFile = VirtualFileManager.getInstance().findFileByUrl(uri);
            if (virtualFile != null) {
                return PsiManager.getInstance(project).findFile(virtualFile);
            }
            return null;
        });
    }


}
