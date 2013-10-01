# openam-auth-sample

OpenAM Sample Custom Auth Module

Need to update [Customizing Authentication Modules](http://openam.forgerock.org/openam-documentation/openam-doc-source/doc/dev-guide/index/chap-auth-spi.html)
to fit with this version of the sample.

1.	`mvn install`
2.	`cp target/openam-auth-sample-1.0.0-SNAPSHOT.jar /path/to/tomcat/webapps/openam/WEB-INF/lib/`
3.	Configure as described in the chapter but with module name `org.forgerock.openam.examples.SampleAuth`
4.	Test with user `demo` password `changeit` for success, or users `test1` and `test2` for failure.

TBC...
