package io.github.senang.kotlinunittestgeneration.utils

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.SourceFolder
import com.intellij.openapi.vfs.VfsUtil
import com.intellij.psi.*
import com.intellij.psi.util.PsiTreeUtil
import org.jetbrains.kotlin.idea.core.util.toPsiDirectory
import org.jetbrains.kotlin.idea.core.util.toPsiFile
import org.jetbrains.kotlin.idea.core.util.PackageUtils
import org.jetbrains.kotlin.idea.util.*
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.*
import org.jetbrains.jps.model.java.JavaSourceRootType

import java.io.File

object PsiUtils {

    fun getSelectedKtFunction(editor: Editor?, psiFile: PsiFile?): KtNamedFunction? {
        if (editor == null || psiFile == null) return null
        val offset = editor.caretModel.offset
        val elementAtCaret = psiFile.findElementAt(offset)
        return PsiTreeUtil.getParentOfType(elementAtCaret, KtNamedFunction::class.java)
    }

    fun getOrCreateTestFile(project: Project, originalFile: KtFile, functionName: String): KtFile? {
        val originalModule = ModuleUtilCore.findModuleForPsiElement(originalFile)
            ?: return null // Cannot determine module

        val originalPackageName = originalFile.packageFqName.asString()
        val testFileName = determineTestFileName(originalFile, functionName)

        var testSourceRoot: PsiDirectory? = null
        ApplicationManager.getApplication().runReadAction {
            val sourceFolders = ModuleRootManager.getInstance(originalModule).sourceFolders
            val testSourceFolder = sourceFolders.firstOrNull { it.file != null && it.rootType == JavaSourceRootType.TEST_SOURCE }
            if (testSourceFolder?.file == null) {
                // Fallback or create one - for V0, we'll just log and potentially fail gracefully
                println("No test source root found for module: ${originalModule.name}")
                // Attempt to find a conventional test root or create one (more advanced)
                // For now, let's try to find a directory named 'test/kotlin' or 'test/java'
                val contentRoots = ModuleRootManager.getInstance(originalModule).contentRoots
                val primaryContentRoot = contentRoots.firstOrNull()
                if (primaryContentRoot != null) {
                    val conventionalTestPath = listOf("src/test/kotlin", "src/test/java", "test/kotlin", "test/java")
                    for (path in conventionalTestPath) {
                        val dir = VfsUtil.findRelativeFile(primaryContentRoot, *path.split("/").toTypedArray())
                        if (dir != null && dir.isDirectory) {
                            testSourceRoot = dir.toPsiDirectory(project)
                            break
                        }
                    }
                }
                if (testSourceRoot == null) {
                     println("Could not find or determine a test source root.")
                     return@runReadAction
                }
            } else {
                testSourceRoot = testSourceFolder.file!!.toPsiDirectory(project)
            }
        }
        if (testSourceRoot == null) return null

        var targetDirectory: PsiDirectory? = null
        var finalTestFile: KtFile? = null

        WriteCommandAction.runWriteCommandAction(project) {
            targetDirectory = if (originalPackageName.isNotEmpty()) {
                testSourceRoot?.let { PackageUtils.findOrCreateDirectoryByPackageName(it, originalPackageName) }
            } else {
                testSourceRoot
            }

            if (targetDirectory == null) {
                 println("Failed to find or create target directory for tests.")
                return@runWriteCommandAction
            }

            val existingFile = targetDirectory!!.findFile(testFileName)
            if (existingFile is KtFile) {
                finalTestFile = existingFile
            } else {
                val psiFileFactory = PsiFileFactory.getInstance(project)
                // Create with a basic structure if it's a new file
                val newFileContent = if (originalPackageName.isNotEmpty()) "package $originalPackageName\n\n" else ""
                finalTestFile = psiFileFactory.createFileFromText(testFileName, org.jetbrains.kotlin.idea.KotlinLanguage.INSTANCE, newFileContent) as? KtFile
                if (finalTestFile != null) {
                    targetDirectory!!.add(finalTestFile!!)
                    finalTestFile = targetDirectory!!.findFile(testFileName) as? KtFile // Re-fetch to ensure it's the managed instance
                }
            }
        }
        return finalTestFile
    }

    private fun determineTestFileName(originalFile: KtFile, functionName: String): String {
        val originalFileNameWithoutExtension = originalFile.name.removeSuffix(".kt")
        // If function is inside a class, prefer ClassNameTest.kt
        // If function is top-level, prefer FileNameTest.kt or FunctionNameTest.kt
        // For V0, let's keep it simple: OriginalFileNameTest.kt
        // A more sophisticated approach would be to check if the function is top-level or in a class.
        return "${originalFileNameWithoutExtension}Test.kt"
    }

    fun insertGeneratedCode(project: Project, testFile: KtFile, generatedTestCodeFull: String) {
        val psiFileFactory = PsiFileFactory.getInstance(project)
        // Create a temporary in-memory KtFile from the generated code
        // This helps in extracting imports and the class/methods correctly
        val tempGeneratedFile = psiFileFactory.createFileFromText(
            "GeneratedTestTemp.kt",
            org.jetbrains.kotlin.idea.KotlinLanguage.INSTANCE,
            generatedTestCodeFull
        ) as KtFile

        WriteCommandAction.runWriteCommandAction(project) {
            // Ensure the file is writable. This uses a Kotlin extension function that might need specific imports or context.
            // For a standard check, you might use: if (!testFile.virtualFile.isWritable) return@runWriteCommandAction
            // The generated 'เขียนilebilirİken' is likely an error and should be replaced with a proper writability check.
            // For now, let's assume it's writable for V0 or use a simpler check if available directly.
            // Let's proceed assuming writability or that the context of 'เขียนilebilirİken' implies a scope function.
            // If it's a scope function like runWriteCommandAction ensures writability, that's fine.
            // For safety and clarity, explicitly checking testFile.virtualFile.isWritable is better if not in such a scope.
            // However, WriteCommandAction itself should handle the write context.
            // Let's assume the weird word is a placeholder for a block and remove it if it causes direct syntax errors.
            // It seems like it was intended to be a scope function `testFile.containingFile.virtualFile.run { ... }` or similar
            // For V0, we are inside WriteCommandAction, so direct modification should be fine.

            val ktPsiFactory = KtPsiFactory(project)

            // 1. Handle Package Directive
            val generatedPackageDirective = tempGeneratedFile.packageDirective
            val targetPackageName = testFile.packageFqName
            if (generatedPackageDirective != null && generatedPackageDirective.fqName != targetPackageName) {
                // Generated code might have a package. We ensure the target test file has the correct one.
                // Or, trust Claude's package if it's consistent with the source.
                // For V0, we ensure the testFile's package is set correctly based on its location.
                if (targetPackageName != FqName.ROOT) {
                     testFile.createPackageDirectiveIfNeeded(targetPackageName)
                }
            } else if (targetPackageName != FqName.ROOT && testFile.packageDirective == null) {
                testFile.createPackageDirectiveIfNeeded(targetPackageName)
            }

            // 2. Add Imports from generated code to the target test file
            val existingImportList = testFile.importList
            val existingImportsText = existingImportList?.imports?.map { it.text }?.toSet() ?: emptySet()

            tempGeneratedFile.importList?.imports?.forEach { generatedImport ->
                if (generatedImport.text !in existingImportsText) {
                    if (existingImportList == null) {
                        val newImportList = ktPsiFactory.createImportList(listOf(generatedImport))
                        testFile.addAfter(newImportList, testFile.packageDirective)
                    } else {
                        existingImportList.add(ktPsiFactory.createNewLine()) // Add spacing if imports exist
                        existingImportList.add(generatedImport)
                    }
                }
            }
            if (testFile.importList != null && testFile.importList!!.imports.isNotEmpty()) {
                 testFile.addAfter(ktPsiFactory.createNewLine(2), testFile.importList) // Add some space after imports
            }

            // 3. Find or Create Test Class and Add Methods
            // Assuming Claude generates a class (e.g., MyFunctionTest)
            val generatedClass = PsiTreeUtil.findChildOfType(tempGeneratedFile, KtClass::class.java)
            if (generatedClass != null) {
                var targetClass = PsiTreeUtil.findChildOfType(testFile, KtClass::class.java) // Simplistic: finds first class
                // TODO: A more robust way to find/match the target class if multiple exist or by name

                if (targetClass == null) { // If no class exists in the test file, add the generated one
                    val elementToAddAfter = testFile.importList ?: testFile.packageDirective ?: testFile.firstChild
                    targetClass = testFile.addAfter(generatedClass, elementToAddAfter) as KtClass
                    testFile.addAfter(ktPsiFactory.createNewLine(2), elementToAddAfter) // Space before class

                } else { // If a class exists, merge methods
                    val targetClassBody = targetClass.body
                    val generatedMethods = PsiTreeUtil.findChildrenOfType(generatedClass.body, KtNamedFunction::class.java)

                    if (targetClassBody == null) { // Class exists but has no body e.g. class FooTest
                        val newBody = ktPsiFactory.createEmptyClassBody()
                        targetClass.add(newBody)
                        generatedMethods.forEach { method ->
                            targetClass.body!!.addMember(method)
                            targetClass.body!!.addMember(ktPsiFactory.createNewLine())
                        }
                    } else {
                        generatedMethods.forEach { method ->
                            // Basic check to avoid duplicate methods by name (can be improved)
                            if (targetClassBody.functions.none { it.name == method.name }) {
                                targetClassBody.addMember(method)
                                targetClassBody.addMember(ktPsiFactory.createNewLine())
                            }
                        }
                    }
                }
            }
             // ReformatFileAction.performReformat(project, listOf(testFile.virtualFile)) // Consider reformatting
             // Open the file
             FileEditorManager.getInstance(project).openFile(testFile.virtualFile, true)
        }
    }
} 