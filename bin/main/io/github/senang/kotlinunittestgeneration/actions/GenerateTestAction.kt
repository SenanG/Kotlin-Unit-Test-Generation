package io.github.senang.kotlinunittestgeneration.actions
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.progress.ProgressIndicator
import com.intellij.openapi.progress.Task
import com.intellij.openapi.project.Project
import io.github.senang.kotlinunittestgeneration.services.ClaudeService
import io.github.senang.kotlinunittestgeneration.utils.PsiUtils
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtNamedFunction

class GenerateTestAction : AnAction() {

    override fun update(e: AnActionEvent) {
        super.update(e)
        val project = e.project
        val editor = e.getData(CommonDataKeys.EDITOR)
        val psiFile = e.getData(CommonDataKeys.PSI_FILE)

        // Enable the action only if a Kotlin function is likely selected
        var isEnabled = false
        if (project != null && editor != null && psiFile is KtFile) {
            // A more precise check would be to see if the caret is within a KtNamedFunction
            // For now, just checking if it's a Kotlin file is a basic enablement
            // PsiUtils.getSelectedKtFunction(editor, psiFile) != null // This is a more accurate check
            isEnabled = true // Keep it simple for V0, refine later if a function is NOT selected.
        }
        e.presentation.isEnabledAndVisible = isEnabled
    }

    override fun actionPerformed(e: AnActionEvent) {
        val project = e.project ?: return
        val editor = e.getData(CommonDataKeys.EDITOR) ?: return
        val psiFile = e.getData(CommonDataKeys.PSI_FILE) as? KtFile ?: return

        val selectedFunction: KtNamedFunction? = ApplicationManager.getApplication().runReadAction<KtNamedFunction?> {
             PsiUtils.getSelectedKtFunction(editor, psiFile)
        }

        if (selectedFunction == null) {
            showNotification(project, "No Kotlin function selected or found at caret.", NotificationType.WARNING)
            return
        }

        val functionText = ApplicationManager.getApplication().runReadAction<String> {
            selectedFunction.text
        }
        val functionName = ApplicationManager.getApplication().runReadAction<String> {
             selectedFunction.name ?: "UnnamedFunction"
        }

        // Run network and PSI modification in a background task
        object : Task.Backgroundable(project, "Generating Unit Tests for ${functionName}", true) {
            override fun run(indicator: ProgressIndicator) {
                indicator.isIndeterminate = true
                indicator.text = "Contacting Claude AI for test generation..."

                try {
                    val generatedTestCode = ClaudeService.generateTestsForFunction(functionText)

                    if (generatedTestCode.isNullOrBlank()) {
                        showNotification(project, "Failed to generate tests: Empty response from backend.", NotificationType.ERROR)
                        return
                    }

                    indicator.text = "Inserting generated tests into project..."
                    // Get/Create test file and insert code (Wrap PSI modifications in WriteAction)
                    val targetTestFile = PsiUtils.getOrCreateTestFile(project, psiFile, functionName)

                    if (targetTestFile == null) {
                        showNotification(project, "Could not find or create a test file.", NotificationType.ERROR)
                        return
                    }

                    ApplicationManager.getApplication().invokeLater {
                        // PSI write operations must be on the EDT and in a write action
                        // PsiUtils.insertGeneratedCode already uses WriteCommandAction
                        PsiUtils.insertGeneratedCode(project, targetTestFile, generatedTestCode)
                        showNotification(project, "Successfully generated and inserted tests for '$functionName'.", NotificationType.INFORMATION)
                    }

                } catch (ex: Exception) {
                    // Log full exception for debugging if needed: ex.printStackTrace()
                    showNotification(project, "Error generating tests: ${ex.message}", NotificationType.ERROR)
                    println("Error during test generation: ${ex.stackTraceToString()}")
                }
            }
        }.queue()
    }

    private fun showNotification(project: Project, content: String, type: NotificationType) {
        ApplicationManager.getApplication().invokeLater {
            NotificationGroupManager.getInstance()
                .getNotificationGroup("KotlinTestGenAI.Notifications") // Make sure this group is defined in plugin.xml or created
                .createNotification(content, type)
                .notify(project)
        }
    }
} 