## See https://github.com/casey/just
## Install with $ brew install just
run:
    ./gradlew allTests allRun install
    cd .. && transkribe
ci:
    ./gradlew runOnGitHub
github:
    open https://github.com/LukasAnda/Transkribe/
issues:
    open https://github.com/LukasAnda/Transkribe/issues
prs:
    open https://github.com/LukasAnda/Transkribe/pulls
urls: github issues prs
    echo "URLs opened"
install:
    ./gradlew install
completions:
    ./gradlew completions
brew:
    brew reinstall --debug --verbose --build-from-source kotlin-cli-starter
    brew test kotlin-cli-starter
    brew audit --strict kotlin-cli-starter
    brew audit kotlin-cli-starter --online --new-formula
