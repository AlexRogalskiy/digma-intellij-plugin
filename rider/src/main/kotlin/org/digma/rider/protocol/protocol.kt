@file:JvmName("Protocol")
package org.digma.rider.protocol

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.VirtualFileManager
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import org.jetbrains.annotations.NotNull
import org.jetbrains.annotations.Nullable


/*
the backend send a file with file system protocol: file:///some/path/class.cs
we need to keep is when ever converting to and from psi file
 */

@Nullable
fun pathToPsiFile(file: String, project: Project): PsiFile? {
    var virtualFile: VirtualFile? = VirtualFileManager.getInstance().findFileByUrl(file)
    if (virtualFile != null) {
        return PsiManager.getInstance(project).findFile(virtualFile);
    }
    return null;
}

@NotNull
fun psiFileToPath(psiFile: PsiFile): String {
    return psiFile.virtualFile.url
}