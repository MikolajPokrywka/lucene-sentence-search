# Introduction 
TODO: Give a short introduction of your project. Let this section explain the objectives or the motivation behind this project. 

# Getting Started
## Requirements
Java JDK installation is required. Project is tested with JDK 8.

## Installation process
To build project simply run:

`./gradlew installDist`
which should result in a build target:
`./build/install/tm`

You can run it by passing arguments to  bat script in 
`./build/install/tm/bin/tm.bat --port 8080 --bleu-rescoring-threshold 0.05 --index-dir my_index`

Or you can run it straight from gradle 
`./gradlew run --args="--port 8080 --bleu-rescoring-threshold 0.05 --index-dir my_index"`

In our case we used the jar file from python3 using jpype:

```import jpype
import jpype.imports
from jpype.types import *
jpype.addClassPath('build/libs/tm.jar')
jpype.startJVM(convertStrings=False)
import java.lang
from java.lang import System
from com import LuceneSentenceSearch
```


## API Call examples
```
curl \
--header "Content-Type: application/json" \
--request POST \
--data '{"source":"Hello World !", "target": "Sveika pasaule!", "meta": {"uid": "Artūrs", "srclang": "en"}}' \
http://localhost:8080/save

Response: 
{
  "errorMessage": null,
  "status": "OK"
}
```

```
curl \
--header "Content-Type: application/json" \
--request POST \
--data '{"input":"Hello World !", "meta": {"uid": "Artūrs", "srclang": "en"}}' \
http://localhost:8080/get

Response: 
{
  "sourceContext" : [ "Hello World !", "Hello Worlds !" ],
  "targetContext" : [ "Sveika pasaule!", "Sveiki pasaules!" ],
  "status" : "OK",
  "errorMessage" : null
}

```

```
curl \
--header "Content-Type: application/json" \
--request POST \
--data '{"uid": "Artūrs"}' \
http://localhost:8080/delete

Response: 
{
  "errorMessage": null,
  "status": "OK"
}
```

## Useful Functions
 - `createIndexInDir("/tmp", "lv")` - will initialize a Latvian source language translation memory stored in `/tmp`
  - `addFileToIndex(srcFile, trgFile, "IT")` - will load content of two parallel files in translation memory for domain `IT`
 - `queryTM(String query_sentence, String domain, boolean skipBleuRescorer, int numberOfCandidates)` - will retrieve at most `numberOfCandidates` sentences from TM that are similar with respect to stemmed query TFIDF; if `skipBleuRescorer is `True` then will also use BLEU rescoring to refine results further
