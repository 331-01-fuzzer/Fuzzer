# Fuzzer
SE 331-01 Fuzzer Project  

USAGE:
 * after unpacking the files, navigate to the directory which contains the file fuzzer.jar
 * execute the following command:
 * `java -jar fuzzer.jar <command> <url> [options...]`
 * where <command> is described in the next section, <url> is the url of the website to fuzz (including protocol), and options are described later

COMMANDS:  
  `discover`  Output a comprehensive, human-readable list of all discovered inputs to the system. Techniques include both crawling and guessing.  
  `test`      Discover all inputs, then attempt a list of exploit vectors on those inputs. Report potential vulnerabilities.  

OPTIONS:  
  `--custom-auth=string`     Signal that the fuzzer should use hard-coded authentication for a specific application (e.g. dvwa). Optional.  

  Discover options:  
    `--common-words=file`    Newline-delimited file of common words to be used in page guessing and input guessing. Required.  

  Test options:  
    `--vectors=file`         Newline-delimited file of common exploits to vulnerabilities. Required.  
    `--sensitive=file`       Newline-delimited file data that should never be leaked. It's assumed that this data is in the application's database (e.g. test data), but is not reported in any response. Required.  
    `--random=[true|false]`  When off, try each input to each page systematically.  When on, choose a random page, then a random input field and test all vectors. Default: false.  
    `--slow=500`             Number of milliseconds considered when a response is considered "slow". Default is 500 milliseconds  
  
Examples:  
  # Discover inputs  
  `java -jar fuzzer.jar discover http://localhost:8080 --common-words=mywords.txt`  

  # Discover inputs to DVWA using our hard-coded authentication  
  `java -jar fuzzer.jar discover http://127.0.0.1 --custom-auth=dvwa --common-words=mywords.txt`  

  # Discover and Test DVWA without randomness  
  `java -jar fuzzer.jar test http://localhost:8080 --custom-auth=dvwa --common-words=words.txt --vectors=vectors.txt --sensitive=creditcards.txt --random=false`  
