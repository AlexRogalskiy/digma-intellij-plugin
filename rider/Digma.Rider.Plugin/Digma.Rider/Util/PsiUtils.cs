using JetBrains.Annotations;
using JetBrains.ReSharper.Psi;
using JetBrains.ReSharper.Psi.CSharp;
using JetBrains.ReSharper.Psi.CSharp.Tree;
using JetBrains.RiderTutorials.Utils;

namespace Digma.Rider.Util
{
    public static class PsiUtils
    {

        public static string GetNamespace(ICSharpFunctionDeclaration functionDeclaration)
        {
            //a method may be declared in a file without namespace, so if we can't find the 
            // namespace assume 'global'
            var namespaceDeclaration =
                functionDeclaration.GetParentOfType<ICSharpNamespaceDeclaration>();
            return namespaceDeclaration == null ? "global" : namespaceDeclaration.QualifiedName;
        }

        public static string GetClassName(ICSharpFunctionDeclaration functionDeclaration)
        {
            return functionDeclaration.GetParentOfType<IClassDeclaration>().DeclaredName;
        }

        public static string GetDeclaredName(ICSharpFunctionDeclaration functionDeclaration)
        {
            return functionDeclaration.DeclaredName;
        }

        // public static string GetContainingFile(ICSharpFunctionDeclaration functionDeclaration)
        // {
        //     return functionDeclaration.GetSourceFile().GetLocation().FullPath;
        // }
        
        public static bool IsPsiSourceFileApplicable([NotNull] IPsiSourceFile psiSourceFile)
        {
            var properties = psiSourceFile.Properties;
            var primaryPsiLanguage = psiSourceFile.PrimaryPsiLanguage;
            var isApplicable = !primaryPsiLanguage.IsNullOrUnknown() &&
                               !properties.IsGeneratedFile &&
                               primaryPsiLanguage.Is<CSharpLanguage>() &&
                               properties.ShouldBuildPsi &&
                               properties.ProvidesCodeModel &&
                               !properties.IsNonUserFile;

            return isApplicable;
        }

        
        
        
    }
}