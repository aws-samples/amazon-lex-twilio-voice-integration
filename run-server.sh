set -e # stop script in case of error at any step.

docker build -t twilio:lex-streaming-sample .

docker run -p 8889:8080 twilio:lex-streaming-sample