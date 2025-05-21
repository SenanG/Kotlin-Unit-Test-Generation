package io.github.senang.kotlinunittestgeneration.actions
import com.intellij.openapi.diagnostic.Logger

/**
 * Optional: Can be used for plugin initialization, though for simple action-based plugins,
 * it might not be strictly necessary. For V0, it can be minimal.
 */
class TestGenAIPlugin {
    companion object {
        private val LOG = Logger.getInstance(TestGenAIPlugin::class.java)
    }

    init {
        LOG.info("KotlinTestGenAI Plugin Loaded")
        // You could initialize services here if they required complex setup,
        // but for V0, direct object/static usage is fine for ClaudeService and PsiUtils.
    }
} 