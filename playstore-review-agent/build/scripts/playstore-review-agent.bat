@rem
@rem Copyright 2015 the original author or authors.
@rem
@rem Licensed under the Apache License, Version 2.0 (the "License");
@rem you may not use this file except in compliance with the License.
@rem You may obtain a copy of the License at
@rem
@rem      https://www.apache.org/licenses/LICENSE-2.0
@rem
@rem Unless required by applicable law or agreed to in writing, software
@rem distributed under the License is distributed on an "AS IS" BASIS,
@rem WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
@rem See the License for the specific language governing permissions and
@rem limitations under the License.
@rem
@rem SPDX-License-Identifier: Apache-2.0
@rem

@if "%DEBUG%"=="" @echo off
@rem ##########################################################################
@rem
@rem  playstore-review-agent startup script for Windows
@rem
@rem ##########################################################################

@rem Set local scope for the variables with windows NT shell
if "%OS%"=="Windows_NT" setlocal

set DIRNAME=%~dp0
if "%DIRNAME%"=="" set DIRNAME=.
@rem This is normally unused
set APP_BASE_NAME=%~n0
set APP_HOME=%DIRNAME%..

@rem Resolve any "." and ".." in APP_HOME to make it shorter.
for %%i in ("%APP_HOME%") do set APP_HOME=%%~fi

@rem Add default JVM options here. You can also use JAVA_OPTS and PLAYSTORE_REVIEW_AGENT_OPTS to pass JVM options to this script.
set DEFAULT_JVM_OPTS=

@rem Find java.exe
if defined JAVA_HOME goto findJavaFromJavaHome

set JAVA_EXE=java.exe
%JAVA_EXE% -version >NUL 2>&1
if %ERRORLEVEL% equ 0 goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is not set and no 'java' command could be found in your PATH. 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:findJavaFromJavaHome
set JAVA_HOME=%JAVA_HOME:"=%
set JAVA_EXE=%JAVA_HOME%/bin/java.exe

if exist "%JAVA_EXE%" goto execute

echo. 1>&2
echo ERROR: JAVA_HOME is set to an invalid directory: %JAVA_HOME% 1>&2
echo. 1>&2
echo Please set the JAVA_HOME variable in your environment to match the 1>&2
echo location of your Java installation. 1>&2

goto fail

:execute
@rem Setup the command line

set CLASSPATH=%APP_HOME%\lib\playstore-review-agent-1.0.0.jar;%APP_HOME%\lib\ktor-client-cio-jvm-3.1.1.jar;%APP_HOME%\lib\koog-agents-jvm-0.6.2.jar;%APP_HOME%\lib\ktor-client-apache5-jvm-3.2.2.jar;%APP_HOME%\lib\agents-mcp-jvm-0.6.2.jar;%APP_HOME%\lib\agents-planner-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-llms-all-jvm-0.6.2.jar;%APP_HOME%\lib\agents-features-event-handler-jvm-0.6.2.jar;%APP_HOME%\lib\agents-features-memory-jvm-0.6.2.jar;%APP_HOME%\lib\agents-features-opentelemetry-jvm-0.6.2.jar;%APP_HOME%\lib\agents-features-snapshot-jvm-0.6.2.jar;%APP_HOME%\lib\agents-features-tokenizer-jvm-0.6.2.jar;%APP_HOME%\lib\agents-features-trace-jvm-0.6.2.jar;%APP_HOME%\lib\agents-ext-jvm-0.6.2.jar;%APP_HOME%\lib\agents-core-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-bedrock-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-anthropic-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-google-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-dashscope-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-deepseek-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-mistralai-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-openai-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-openrouter-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-openai-client-base-jvm-0.6.2.jar;%APP_HOME%\lib\http-client-ktor-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-cache-files-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-cache-redis-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-cached-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-cache-model-jvm-0.6.2.jar;%APP_HOME%\lib\embeddings-llm-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-ollama-client-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-llms-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-clients-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-processor-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-structure-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-executor-model-jvm-0.6.2.jar;%APP_HOME%\lib\agents-tools-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-markdown-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-tokenizer-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-xml-jvm-0.6.2.jar;%APP_HOME%\lib\prompt-model-jvm-0.6.2.jar;%APP_HOME%\lib\utils-jvm-0.6.2.jar;%APP_HOME%\lib\http-client-core-jvm-0.6.2.jar;%APP_HOME%\lib\vector-storage-jvm-0.6.2.jar;%APP_HOME%\lib\rag-base-jvm-0.6.2.jar;%APP_HOME%\lib\ktor-network-tls-jvm-3.1.1.jar;%APP_HOME%\lib\kotlinx-coroutines-reactive-1.10.2.jar;%APP_HOME%\lib\ktor-client-content-negotiation-jvm-3.2.2.jar;%APP_HOME%\lib\ktor-serialization-kotlinx-json-jvm-3.2.2.jar;%APP_HOME%\lib\ktor-client-logging-jvm-3.2.2.jar;%APP_HOME%\lib\ktor-server-sse-jvm-3.2.2.jar;%APP_HOME%\lib\ktor-server-cio-jvm-3.2.2.jar;%APP_HOME%\lib\kotlin-sdk-client-jvm-0.8.1.jar;%APP_HOME%\lib\ktor-client-core-jvm-3.2.3.jar;%APP_HOME%\lib\kotlinx-coroutines-slf4j-1.10.2.jar;%APP_HOME%\lib\ktor-serialization-kotlinx-jvm-3.2.2.jar;%APP_HOME%\lib\kotlin-sdk-core-jvm-0.8.1.jar;%APP_HOME%\lib\bedrockruntime-jvm-1.5.123.jar;%APP_HOME%\lib\aws-config-jvm-1.5.123.jar;%APP_HOME%\lib\aws-http-jvm-1.5.123.jar;%APP_HOME%\lib\aws-endpoint-jvm-1.5.123.jar;%APP_HOME%\lib\aws-json-protocols-jvm-1.5.27.jar;%APP_HOME%\lib\aws-xml-protocols-jvm-1.5.27.jar;%APP_HOME%\lib\aws-protocol-core-jvm-1.5.27.jar;%APP_HOME%\lib\aws-event-stream-jvm-1.5.27.jar;%APP_HOME%\lib\aws-signing-default-jvm-1.5.27.jar;%APP_HOME%\lib\http-auth-aws-jvm-1.5.27.jar;%APP_HOME%\lib\aws-signing-common-jvm-1.5.27.jar;%APP_HOME%\lib\http-client-engine-default-jvm-1.5.27.jar;%APP_HOME%\lib\http-client-engine-okhttp-jvm-1.5.27.jar;%APP_HOME%\lib\http-client-jvm-1.5.27.jar;%APP_HOME%\lib\aws-core-jvm-1.5.123.jar;%APP_HOME%\lib\smithy-client-jvm-1.5.27.jar;%APP_HOME%\lib\aws-credentials-jvm-1.5.27.jar;%APP_HOME%\lib\http-auth-jvm-1.5.27.jar;%APP_HOME%\lib\http-auth-api-jvm-1.5.27.jar;%APP_HOME%\lib\http-jvm-1.5.27.jar;%APP_HOME%\lib\identity-api-jvm-1.5.27.jar;%APP_HOME%\lib\telemetry-defaults-jvm-1.5.27.jar;%APP_HOME%\lib\logging-slf4j2-jvm-1.5.27.jar;%APP_HOME%\lib\telemetry-api-jvm-1.5.27.jar;%APP_HOME%\lib\serde-json-jvm-1.5.27.jar;%APP_HOME%\lib\serde-xml-jvm-1.5.27.jar;%APP_HOME%\lib\serde-form-url-jvm-1.5.27.jar;%APP_HOME%\lib\serde-jvm-1.5.27.jar;%APP_HOME%\lib\runtime-core-jvm-1.5.27.jar;%APP_HOME%\lib\ktor-server-websockets-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-server-core-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-http-cio-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-websocket-serialization-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-serialization-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-websockets-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-http-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-events-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-sse-jvm-3.2.3.jar;%APP_HOME%\lib\okhttp-coroutines-5.1.0.jar;%APP_HOME%\lib\ktor-network-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-utils-jvm-3.2.3.jar;%APP_HOME%\lib\ktor-io-jvm-3.2.3.jar;%APP_HOME%\lib\kotlinx-coroutines-core-jvm-1.10.2.jar;%APP_HOME%\lib\kotlinx-coroutines-jdk8-1.10.2.jar;%APP_HOME%\lib\prompt-llm-jvm-0.6.2.jar;%APP_HOME%\lib\agents-utils-jvm-0.6.2.jar;%APP_HOME%\lib\embeddings-base-jvm-0.6.2.jar;%APP_HOME%\lib\kotlin-reflect-2.2.10.jar;%APP_HOME%\lib\kotlinx-datetime-jvm-0.6.2.jar;%APP_HOME%\lib\kotlin-logging-jvm-7.0.13.jar;%APP_HOME%\lib\kotlinx-serialization-json-io-jvm-1.9.0.jar;%APP_HOME%\lib\kotlinx-serialization-core-jvm-1.9.0.jar;%APP_HOME%\lib\kotlinx-serialization-json-jvm-1.9.0.jar;%APP_HOME%\lib\kotlinx-io-core-jvm-0.8.2.jar;%APP_HOME%\lib\kotlinx-collections-immutable-jvm-0.4.0.jar;%APP_HOME%\lib\opentelemetry-exporter-otlp-1.51.0.jar;%APP_HOME%\lib\opentelemetry-exporter-sender-okhttp-1.51.0.jar;%APP_HOME%\lib\okhttp-jvm-5.1.0.jar;%APP_HOME%\lib\okio-jvm-3.16.0.jar;%APP_HOME%\lib\kotlinx-io-bytestring-jvm-0.8.2.jar;%APP_HOME%\lib\kotlin-stdlib-2.2.21.jar;%APP_HOME%\lib\google-api-services-androidpublisher-v3-rev20260129-2.0.0.jar;%APP_HOME%\lib\google-api-client-2.7.2.jar;%APP_HOME%\lib\google-auth-library-oauth2-http-1.30.0.jar;%APP_HOME%\lib\google-http-client-jackson2-1.46.0.jar;%APP_HOME%\lib\slf4j-simple-2.0.17.jar;%APP_HOME%\lib\annotations-26.0.2-1.jar;%APP_HOME%\lib\auto-value-annotations-1.11.0.jar;%APP_HOME%\lib\google-oauth-client-1.36.0.jar;%APP_HOME%\lib\google-http-client-gson-1.45.2.jar;%APP_HOME%\lib\google-http-client-apache-v2-1.45.2.jar;%APP_HOME%\lib\google-http-client-1.46.0.jar;%APP_HOME%\lib\opencensus-contrib-http-util-0.31.1.jar;%APP_HOME%\lib\guava-33.3.1-jre.jar;%APP_HOME%\lib\jsr305-3.0.2.jar;%APP_HOME%\lib\google-auth-library-credentials-1.30.0.jar;%APP_HOME%\lib\error_prone_annotations-2.36.0.jar;%APP_HOME%\lib\jackson-core-2.18.2.jar;%APP_HOME%\lib\httpclient5-5.5.jar;%APP_HOME%\lib\slf4j-api-2.0.17.jar;%APP_HOME%\lib\httpclient-4.5.14.jar;%APP_HOME%\lib\commons-codec-1.17.1.jar;%APP_HOME%\lib\httpcore-4.4.16.jar;%APP_HOME%\lib\j2objc-annotations-3.0.0.jar;%APP_HOME%\lib\opencensus-api-0.31.1.jar;%APP_HOME%\lib\grpc-context-1.70.0.jar;%APP_HOME%\lib\gson-2.11.0.jar;%APP_HOME%\lib\commons-logging-1.2.jar;%APP_HOME%\lib\grpc-api-1.70.0.jar;%APP_HOME%\lib\opentelemetry-exporter-logging-1.51.0.jar;%APP_HOME%\lib\opentelemetry-exporter-otlp-common-1.51.0.jar;%APP_HOME%\lib\opentelemetry-exporter-common-1.51.0.jar;%APP_HOME%\lib\opentelemetry-sdk-extension-autoconfigure-spi-1.51.0.jar;%APP_HOME%\lib\opentelemetry-sdk-1.51.0.jar;%APP_HOME%\lib\lettuce-core-6.5.5.RELEASE.jar;%APP_HOME%\lib\failureaccess-1.0.2.jar;%APP_HOME%\lib\listenablefuture-9999.0-empty-to-avoid-conflict-with-guava.jar;%APP_HOME%\lib\checker-qual-3.43.0.jar;%APP_HOME%\lib\httpcore5-h2-5.3.4.jar;%APP_HOME%\lib\httpcore5-5.3.4.jar;%APP_HOME%\lib\opentelemetry-sdk-trace-1.51.0.jar;%APP_HOME%\lib\opentelemetry-sdk-metrics-1.51.0.jar;%APP_HOME%\lib\opentelemetry-sdk-logs-1.51.0.jar;%APP_HOME%\lib\opentelemetry-sdk-common-1.51.0.jar;%APP_HOME%\lib\opentelemetry-api-1.51.0.jar;%APP_HOME%\lib\opentelemetry-context-1.51.0.jar;%APP_HOME%\lib\netty-handler-4.1.118.Final.jar;%APP_HOME%\lib\netty-transport-native-unix-common-4.1.118.Final.jar;%APP_HOME%\lib\netty-codec-4.1.118.Final.jar;%APP_HOME%\lib\netty-transport-4.1.118.Final.jar;%APP_HOME%\lib\netty-resolver-4.1.118.Final.jar;%APP_HOME%\lib\netty-buffer-4.1.118.Final.jar;%APP_HOME%\lib\netty-common-4.1.118.Final.jar;%APP_HOME%\lib\reactor-core-3.6.6.jar;%APP_HOME%\lib\reactive-streams-1.0.4.jar;%APP_HOME%\lib\config-1.4.3.jar;%APP_HOME%\lib\jansi-2.4.2.jar


@rem Execute playstore-review-agent
"%JAVA_EXE%" %DEFAULT_JVM_OPTS% %JAVA_OPTS% %PLAYSTORE_REVIEW_AGENT_OPTS%  -classpath "%CLASSPATH%" com.ai.aidicted.agent.MainKt %*

:end
@rem End local scope for the variables with windows NT shell
if %ERRORLEVEL% equ 0 goto mainEnd

:fail
rem Set variable PLAYSTORE_REVIEW_AGENT_EXIT_CONSOLE if you need the _script_ return code instead of
rem the _cmd.exe /c_ return code!
set EXIT_CODE=%ERRORLEVEL%
if %EXIT_CODE% equ 0 set EXIT_CODE=1
if not ""=="%PLAYSTORE_REVIEW_AGENT_EXIT_CONSOLE%" exit %EXIT_CODE%
exit /b %EXIT_CODE%

:mainEnd
if "%OS%"=="Windows_NT" endlocal

:omega
