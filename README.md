# spark-pac4j-demo-fb-m18

This demo started with a copy of <https://github.com/pac4j/spark-pac4j-demo> and then tried to strip it down to only what is needed for Facebook.

# Below this, README contents from original repo

<p align="center">
  <img src="https://pac4j.github.io/pac4j/img/logo-spark.png" width="300" />
</p>

This `spark-pac4j-demo` project is a SparkJava application to test the [spark-pac4j](https://github.com/pac4j/spark-pac4j) security library with various authentication mechanisms: Facebook, Twitter, form, basic auth, CAS, SAML, OpenID Connect, JWT...

## Start & test

Build the project and launch the SparkJava app on [http://localhost:8080](http://localhost:8080):

    cd spark-pac4j-demo
    mvn clean compile exec:java

To test, you can call a protected url by clicking on the "Protected url by **xxx**" link, which will start the authentication process with the **xxx** provider.
