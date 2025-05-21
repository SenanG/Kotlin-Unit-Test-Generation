DEFAULT_PROMPT_TEMPLATE = """
You are an expert Kotlin developer specializing in writing concise and effective JUnit5 unit tests.

Your task is to generate a compilable JUnit5 test class for the provided Kotlin function.

Please ensure the following:
1.  The output should be a complete Kotlin file content, including necessary package declaration (if the original function has one) and all required import statements for JUnit5 (e.g., `org.junit.jupiter.api.Test`, `org.junit.jupiter.api.Assertions.*`).
2.  The test class name should be derived from the original class name (if any) or the function name, appended with "Test". For example, if the function is in `MyUtils.kt`, the test class might be `MyUtilsTest`. If the function is top-level in `utils.kt` and named `doSomething`, the test class could be `DoSomethingTest`.
3.  Include a test method for the happy path.
4.  Include test methods for common edge cases (e.g., empty inputs for collections/strings if applicable, zero/negative numbers for numerical inputs, nulls if the function parameters are nullable, though prefer non-nullable Kotlin types).
5.  Use appropriate JUnit5 assertions (e.g., `assertEquals`, `assertTrue`, `assertThrows`).
6.  The generated code should be clean, readable, and directly usable.
7.  Do NOT include any explanations or comments outside of the code itself (like "Here is your test code:"). Only provide the raw Kotlin code for the test file.

Original Kotlin function:
```kotlin
{kotlin_function_code}
```

Begin Test Code:
"""

def get_prompt(function_code: str) -> str:
    """
    Constructs the prompt for Claude with the given Kotlin function code.
    """
    return DEFAULT_PROMPT_TEMPLATE.format(kotlin_function_code=function_code) 