from fastapi import FastAPI, HTTPException, Body
from pydantic import BaseModel, Field
from anthropic import AsyncAnthropic
import os
from dotenv import load_dotenv

from .prompt_templates import get_prompt

# Load environment variables from .env file
load_dotenv()

app = FastAPI(
    title="KotlinTestGenAI Backend",
    description="API for generating Kotlin JUnit5 tests using Anthropic Claude.",
    version="0.1.0"
)

# Initialize Anthropic client
# The API key is read automatically from the ANTHROPIC_API_KEY environment variable
try:
    anthropic_client = AsyncAnthropic()
except Exception as e:
    print(f"Failed to initialize Anthropic client: {e}")
    # Depending on policy, you might want to exit or run with a dummy client for local dev without real API calls
    anthropic_client = None


class GenerateTestRequest(BaseModel):
    code: str = Field(..., description="The Kotlin function code to generate tests for.", example="fun add(a: Int, b: Int): Int { return a + b }")
    # Potentially add context like file name, package name for more accurate test generation
    # file_name: str | None = Field(None, description="Original file name of the Kotlin code")
    # package_name: str | None = Field(None, description="Package name of the Kotlin code")

class GenerateTestResponse(BaseModel):
    generated_test_code: str = Field(..., description="The generated JUnit5 test code as a string.")
    model_used: str = Field(default="claude-3-5-sonnet-20240620", description="The Claude model used for generation.") # Or make this configurable

@app.post("/generate", response_model=GenerateTestResponse)
async def generate_tests_endpoint(request_data: GenerateTestRequest):
    """
    Receives Kotlin function code, generates JUnit5 tests using Claude, and returns the test code.
    """
    if not anthropic_client:
        raise HTTPException(status_code=503, detail="Anthropic client not initialized. Check API key and server logs.")

    if not request_data.code or request_data.code.isspace():
        raise HTTPException(status_code=400, detail="Input code cannot be empty.")

    prompt = get_prompt(request_data.code)
    model_to_use = "claude-3-5-sonnet-20240620" # Consider making this configurable or dynamic

    try:
        print(f"Sending request to Claude for code snippet: \n{request_data.code[:200]}...") # Log snippet
        
        response = await anthropic_client.messages.create(
            model=model_to_use,
            max_tokens=2048, # Adjust as needed, consider the complexity of tests
            messages=[
                {
                    "role": "user",
                    "content": prompt
                }
            ]
        )
        
        generated_code = ""
        if response.content and isinstance(response.content, list):
            for block in response.content:
                if hasattr(block, 'text'):
                    generated_code += block.text
        
        if not generated_code.strip():
            print("Claude returned an empty response.")
            raise HTTPException(status_code=500, detail="Claude returned an empty response.")

        # Basic cleanup: Sometimes Claude might include the "Begin Test Code:" part or triple backticks
        generated_code = generated_code.split("Begin Test Code:")[-1].strip()
        if generated_code.startswith("```kotlin"):
            generated_code = generated_code[len("```kotlin"):].strip()
        if generated_code.endswith("```"):
            generated_code = generated_code[:-len("```")].strip()
            
        print(f"Successfully received response from Claude.")
        return GenerateTestResponse(generated_test_code=generated_code, model_used=model_to_use)

    except Exception as e:
        print(f"Error calling Anthropic API: {e}") # Log the full error
        raise HTTPException(status_code=500, detail=f"Error communicating with Claude API: {str(e)}")

@app.get("/health", summary="Health Check", description="Simple health check endpoint.")
async def health_check():
    return {"status": "ok"}

if __name__ == "__main__":
    import uvicorn
    # This is for direct execution (python app.py), though uvicorn app:app is preferred for --reload
    uvicorn.run(app, host="0.0.0.0", port=8000) 