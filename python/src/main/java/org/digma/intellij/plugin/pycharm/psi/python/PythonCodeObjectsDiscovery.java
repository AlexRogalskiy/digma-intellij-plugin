package org.digma.intellij.plugin.pycharm.psi.python;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.psi.search.searches.ReferencesSearch;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.Query;
import com.jetbrains.python.psi.*;
import com.jetbrains.python.psi.stubs.PyFunctionNameIndex;
import org.apache.commons.lang3.time.StopWatch;
import org.digma.intellij.plugin.log.Log;
import org.digma.intellij.plugin.model.discovery.DocumentInfo;
import org.digma.intellij.plugin.model.discovery.MethodInfo;
import org.digma.intellij.plugin.model.discovery.SpanInfo;
import org.digma.intellij.plugin.psi.PsiUtils;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.util.*;
import java.util.concurrent.TimeUnit;

public class PythonCodeObjectsDiscovery {

    private static final Logger LOGGER = Logger.getInstance(PythonCodeObjectsDiscovery.class);

    private static final String UNKNOWN_INST_LIBRARY = "UNKNOWN_INST_LIBRARY";
    private static final String UNKNOWN_SPAN_NAME = "UNKNOWN_SPAN_NAME";

    public static @NotNull DocumentInfo buildDocumentInfo(@NotNull Project project, @NotNull PyFile pyFile) {
        var stopWatch = StopWatch.createStarted();

        try {
            return buildDocumentInfoImpl(project, pyFile);
        } finally {
            stopWatch.stop();
            Log.log(LOGGER::debug, "buildDocumentInfo for {} took {} milliseconds", pyFile.getName(), stopWatch.getTime(TimeUnit.MILLISECONDS));
        }
    }


    private static @NotNull DocumentInfo buildDocumentInfoImpl(@NotNull Project project, @NotNull PyFile pyFile) {

        var fileUri = PsiUtils.psiFileToUri(pyFile);
        var methods = new HashMap<String, MethodInfo>();

        var functions = pyFile.getTopLevelFunctions();
        var classes = pyFile.getTopLevelClasses();

        classes.forEach(pyClass -> {
            for (PyFunction method : pyClass.getMethods()) {
                //function name should probably never be null but the interface is Nullable so we need to check
                if (method.getName() != null) {
                    MethodInfo methodInfo = functionDiscovery(project, fileUri, method);
                    methods.put(methodInfo.getId(), methodInfo);
                }
            }
        });

        functions.forEach(pyFunction -> {
            //function name should probably never be null but the interface is Nullable so we need to check
            if (pyFunction.getName() != null) {
                MethodInfo methodInfo = functionDiscovery(project, fileUri, pyFunction);
                methods.put(methodInfo.getId(), methodInfo);
            }
        });

        spanDiscovery(project, pyFile, fileUri, methods);
        return new DocumentInfo(fileUri, methods);
    }


    private static MethodInfo functionDiscovery(@NotNull Project project, @NotNull String fileUri, @NotNull PyFunction pyFunction) {
        Objects.requireNonNull(pyFunction);
        Objects.requireNonNull(pyFunction.getName());

        var methodId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, pyFunction);

        var name = pyFunction.getName();
        var className = pyFunction.getContainingClass() == null ? "" : pyFunction.getContainingClass().getName() == null ? "" : pyFunction.getContainingClass().getName();
        var namespace = pyFunction.getQualifiedName() == null ? "" : pyFunction.getQualifiedName().substring(0, pyFunction.getQualifiedName().lastIndexOf("."));

        var methodInfo = new MethodInfo(methodId, name, className, namespace, fileUri, pyFunction.getTextOffset(), new ArrayList<>());
        methodInfo.setAdditionalIdsProvider(new PythonAdditionalIdsProvider());
        return methodInfo;
    }


    private static void spanDiscovery(@NotNull Project project, @NotNull PyFile pyFile, String fileUri, @NotNull Map<String, MethodInfo> methodInfoMap) {
        spanDiscovery(project, pyFile, fileUri, Constants.OPENTELEMETRY_START_AS_CURRENT_SPAN_FUNC_NAME, methodInfoMap);
        spanDiscovery(project, pyFile, fileUri, Constants.OPENTELEMETRY_START_SPAN_FUNC_NAME, methodInfoMap);
    }

    private static void spanDiscovery(@NotNull Project project, @NotNull PyFile pyFile, String fileUri, @NotNull String tracerMethodName, @NotNull Map<String, MethodInfo> methodInfoMap) {

        var functions = PyFunctionNameIndex.find(tracerMethodName, project);

        //for some reason the search returns two identical functions, so just choose the first one.
        // I expect only one and don't know why there are two.
        PyFunction startSpanFunction = functions.stream().filter(pyFunction -> pyFunction.getContainingClass() != null &&
                Constants.OPENTELEMETRY_TRACER_FQN.equals(pyFunction.getContainingClass().getQualifiedName())).findFirst().orElse(null);

        if (startSpanFunction == null) {
            return;
        }

        Query<PsiReference> references = ReferencesSearch.search(startSpanFunction, GlobalSearchScope.fileScope(pyFile));
        references.forEach(psiReference -> {
            Log.log(LOGGER::debug, "found reference to {} function {}", tracerMethodName, psiReference.getElement().getText());
            var pyCallExpression = PsiTreeUtil.getParentOfType(psiReference.getElement(), PyCallExpression.class);
            if (pyCallExpression != null) {
                Log.log(LOGGER::debug, "call expression to {} function is {} ", tracerMethodName, pyCallExpression.getText());
                //span discovery may return more than one SpanInfo, but they all should belong
                // to the same method. it happens when a span has more than one id.
                List<SpanInfo> spanInfos = discoverSpanFromStartSpanMethodCallExpression(project, pyFile, pyCallExpression, fileUri);
                if (spanInfos.size() > 0) {
                    MethodInfo methodInfo = methodInfoMap.get(spanInfos.get(0).getContainingMethodId());
                    methodInfo.getSpans().addAll(spanInfos);
                }
            }
        });
    }


    /**
     * this method discovers a span from a call to one of the open telemetry start span methods.
     * the call expression is something like tracer.start_span("span name")
     */
    @NotNull
    private static List<SpanInfo> discoverSpanFromStartSpanMethodCallExpression(@NotNull Project project, @NotNull PyFile pyFile, @NotNull PyCallExpression pyCallExpression, String fileUri) {

        var result = new ArrayList<SpanInfo>();
        var args = pyCallExpression.getArguments();
        var function = PsiTreeUtil.getParentOfType(pyCallExpression, PyFunction.class);

        //don't know how to compute span name if there are no arguments or if receiver is null or if the span is not inside a function block
        if (args.length >= 1 && function != null && pyCallExpression.getReceiver(null) != null) {

            var methodId = PythonLanguageUtils.createPythonMethodCodeObjectId(project, function);
            //span name is always the first argument to start_span or start_as_current_span
            var spanName = getSpanNameFromNameArgument(args[0]);
            //the receiver is the tracer object in this expression: tracer.start_span
            var receiver = pyCallExpression.getReceiver(null);
            //receiver should not be null, tested above
            var instLibrary = getInstLibraryFromReceiverExpression(Objects.requireNonNull(receiver));

            //in case the tracer name is the __name__ variable, create 3 spans with different instrumentation library names
            if (Constants.PYTHON_MODULE_NAME_VARIABLE.equals(instLibrary)) {

                //first instrumentation library name is "__main__"
                var spanId = PythonLanguageUtils.createSpanId("__main__", spanName);
                result.add(new SpanInfo(spanId, spanName, methodId, fileUri));

                //second instrumentation library is the relative path from project root ,without the py extension.
                var path = pyFile.getVirtualFile().getPath();
                var index = path.lastIndexOf(project.getName());
                path = path.substring(index + project.getName().length());
                if (path.startsWith(File.separator)) {
                    path = path.substring(1);
                }
                path = path.substring(0, path.lastIndexOf(".py")).replace(File.separator, "."); //should work on linux and windows
                spanId = PythonLanguageUtils.createSpanId(path, spanName);
                result.add(new SpanInfo(spanId, spanName, methodId, fileUri));

                //third instrumentation library is the relative path without its first segment if any
                if (path.indexOf(".") > 0) {
                    path = path.substring(path.indexOf(".") + 1);
                    spanId = PythonLanguageUtils.createSpanId(path, spanName);
                    result.add(new SpanInfo(spanId, spanName, methodId, fileUri));
                }

            } else {
                var spanId = PythonLanguageUtils.createSpanId(instLibrary, spanName);
                result.add(new SpanInfo(spanId, spanName, methodId, fileUri));
            }
        }


        return result;
    }


    //receiver expression is the tracer in tracer.start_span()
    @NotNull
    private static String getInstLibraryFromReceiverExpression(@NotNull PyExpression receiver) {

        if (receiver instanceof PyReferenceExpression referenceExpression) {
            return getInstLibraryFromReferenceExpression(referenceExpression);
        }else if(receiver instanceof PyCallExpression callExpression){
            return getInstLibraryFromCallExpression(callExpression);
        }

        return UNKNOWN_INST_LIBRARY;
    }


    private static String getInstLibraryFromReferenceExpression(PyReferenceExpression referenceExpression) {

        var psiElement = referenceExpression.getReference().resolve();
        if (psiElement != null) {
            PsiElement context = psiElement.getContext();
            if (context instanceof PyAssignmentStatement assignmentStatement) {
                return getInstLibraryFromAssignmentStatement(assignmentStatement);
            }
        }

        return UNKNOWN_INST_LIBRARY;
    }

    //assignmentStatement is the assignment in tracer = trace.get_tracer
    private static String getInstLibraryFromAssignmentStatement(PyAssignmentStatement assignmentStatement) {
        var assignedValue = assignmentStatement.getAssignedValue();
        if (assignedValue instanceof PyCallExpression callExpression) {
            return getInstLibraryFromCallExpression(callExpression);
        }

        return UNKNOWN_INST_LIBRARY;
    }

    //callExpression is the call to trace.get_tracer, we want the argument of get_tracer
    private static String getInstLibraryFromCallExpression(PyCallExpression callExpression) {
        var callee = callExpression.getCallee();
        if (callee != null && callee.getReference() != null) {
            var psiElement = callee.getReference().resolve();
            if (psiElement instanceof PyFunction && Constants.OPENTELEMETRY_GET_TRACER_FUNC_NAME.equals(((PyFunction) psiElement).getName())) {
                var arguments = callExpression.getArguments();
                if (arguments.length >= 1) {
                    var arg = arguments[0];
                    return getInstLibraryFromGetTracerArgument(arg);
                }
            }
        }

        return UNKNOWN_INST_LIBRARY;
    }


    private static String getInstLibraryFromGetTracerArgument(PyExpression arg) {
        if (arg instanceof PyStringLiteralExpression stringLiteralExpression) {
            return getStringFromStringLiteralExpression(stringLiteralExpression);
        } else if (arg instanceof PyReferenceExpression referenceExpression) {
            return getInstLibraryFromArgumentReferenceExpression(referenceExpression);
        }

        return UNKNOWN_INST_LIBRARY;
    }


    private static String getInstLibraryFromArgumentReferenceExpression(PyReferenceExpression referenceExpression) {

        if (Constants.PYTHON_MODULE_NAME_VARIABLE.equals(referenceExpression.getText())) {
            return Constants.PYTHON_MODULE_NAME_VARIABLE;
        }

        var pyElement = referenceExpression.getReference().resolve();
        if (pyElement instanceof PyTargetExpression targetExpression){
            var assignedValue = targetExpression.findAssignedValue();
            if (assignedValue instanceof PyStringLiteralExpression stringLiteralExpression){
                return getStringFromStringLiteralExpression(stringLiteralExpression);
            }else if (assignedValue instanceof PyReferenceExpression){
                return getInstLibraryFromArgumentReferenceExpression((PyReferenceExpression) assignedValue);
            }
        }

        return UNKNOWN_INST_LIBRARY;
    }


    @NotNull
    private static String getSpanNameFromNameArgument(PyExpression pyExpression) {
        if (pyExpression instanceof PyStringLiteralExpression stringLiteralExpression) {
            return getStringFromStringLiteralExpression(stringLiteralExpression);
        }else if (pyExpression instanceof PyReferenceExpression referenceExpression){
            return getSpanNameFromReferenceExpression(referenceExpression);
        }

        return UNKNOWN_SPAN_NAME;
    }

    private static String getSpanNameFromReferenceExpression(PyReferenceExpression referenceExpression) {

        if (Constants.PYTHON_MODULE_NAME_VARIABLE.equals(referenceExpression.getText())) {
            return Constants.PYTHON_MODULE_NAME_VARIABLE;
        }

        var pyElement = referenceExpression.getReference().resolve();
        if (pyElement instanceof PyTargetExpression targetExpression){
            var assignedValue = targetExpression.findAssignedValue();
            if (assignedValue instanceof PyStringLiteralExpression stringLiteralExpression){
                return getStringFromStringLiteralExpression(stringLiteralExpression);
            }else if (assignedValue instanceof PyReferenceExpression){
                return getSpanNameFromReferenceExpression((PyReferenceExpression) assignedValue);
            }
        }

        return UNKNOWN_SPAN_NAME;
    }


    @NotNull
    private static String getStringFromStringLiteralExpression(PyStringLiteralExpression stringLiteralExpression) {
        return stringLiteralExpression.getStringValue();
    }


//    private static List<SpanInfo> discoverSpansTMP(Project project, PyFunction pyFunction) {
//
//
//        var functions = PyFunctionNameIndex.find(Constants.OPENTELEMETRY_START_AS_CURRENT_SPAN_FUNC_NAME, project);
//        functions.forEach(startSpanFunction -> {
//            if (startSpanFunction.getContainingClass() != null &&
//                    Constants.OPENTELEMETRY_TRACER_FQN.equals(startSpanFunction.getContainingClass().getQualifiedName())) {
//                System.out.println("found my function");
//                Query<PsiReference> references = ReferencesSearch.search(startSpanFunction, GlobalSearchScope.allScope(project));
//                references.forEach(psiReference -> {
//                    var pyCallExpression = PsiTreeUtil.getParentOfType(psiReference.getElement(), PyCallExpression.class);
//                    var pyExpression = pyCallExpression.getReceiver(null);
//                    var args = pyCallExpression.getArguments();
//                    System.out.println();
//
//                });
//            }
//        });
//
//
//        PyRecursiveElementVisitor pyRecursiveElementVisitor = new PyRecursiveElementVisitor() {
//            @Override
//            public void visitElement(@NotNull PsiElement element) {
//                super.visitElement(element);
//            }
//
//            @Override
//            public void visitPyReferenceExpression(@NotNull PyReferenceExpression node) {
//                super.visitPyReferenceExpression(node);
//            }
//
//            @Override
//            public void visitPyTargetExpression(@NotNull PyTargetExpression node) {
//                super.visitPyTargetExpression(node);
//            }
//
//            @Override
//            public void visitPyCallExpression(@NotNull PyCallExpression node) {
//                super.visitPyCallExpression(node);
//                //((PyTargetExpression)((PyAssignmentStatement)node.getReceiver(null).getReference().resolve().getContext()).getLeftHandSideExpression())
//            }
//
//            @Override
//            public void visitPyAssignmentStatement(@NotNull PyAssignmentStatement node) {
//                super.visitPyAssignmentStatement(node);
//            }
//        };
//
//        pyFunction.accept(pyRecursiveElementVisitor);
//
//
//        //PsiTreeUtil
////        ReferencesSearch.search()
////        new PyStringReferenceSearch().processQuery();
////        PySearchUtil.computeElementNameForStringSearch()
////        PyPsiIndexUtil.findUsages()
//
//
////        var traceClass = findTracerClass(project)
//
//        //PyFunction pyFunction1 = ((PyReferenceExpression)((PyWithStatement)pyFunction.getChildren()[2].getChildren()[0]).getWithItems()[0].getChildren()[0].getChildren()[0]).getReference().resolve();
//
//        //((((PyCallExpression)((PyWithStatement)pyFunction.getChildren()[2].getChildren()[0]).getWithItems()[0].getChildren()[0]).getCallee().getChildren()[0].getReference().resolve().getReference().resolve()).getContext().getChildren()[0]
//        return new ArrayList<>();
//    }

}
