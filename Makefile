dupfind.jar: *.java
	javac DupFind.java Tokenizer.java
	jar -cmvf dupfind.inf dupfind.jar *.class
	rm -f *.class
