# Introduction 
TODO: Give a short introduction of your project. Let this section explain the objectives or the motivation behind this project. 

# Getting Started
To build project simply run:

`./gradlew installDist`
which should result in a build target:

`build/libs/tm.jar`

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

## Useful Functions
 - `createIndexInDir("/tmp", "lv")` - will initialize a Latvian source language translation memory stored in `/tmp`
  - `addFileToIndex(srcFile, trgFile, "IT")` - will load content of two parallel files in translation memory for domain `IT`
 - `queryTM(String query_sentence, String domain, boolean skipBleuRescorer, int numberOfCandidates)` - will retrieve atmost `numberOfCandidates` sentences from TM that are similar with respect to stemmed query TFIDF; if `skipBleuRescorer is `True` then will also use BLEU rescoring to further refine results
