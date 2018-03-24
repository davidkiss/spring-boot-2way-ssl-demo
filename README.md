# Overview
This is a sample Spring Boot project that configures an ```javax.net.ssl.SSLContext``` object with two-way SSL using a keystore and an optional truststore.

The ```javax.net.ssl.SSLContext``` object then can be used to create SOAP clients, REST clients, etc. that need 2-way SSL.

# Dependencies
I found that below library is more robust for creating ```KeyStore``` objects in Java than using the JDK's mechanism:
```xml
<dependency>
    <groupId>ca.juliusdavies</groupId>
    <artifactId>not-yet-commons-ssl</artifactId>
    <version>0.3.11</version>
</dependency>
```

# Config properties
To configure 2-way SSL, we need to edit the ```src/main/resources/application.yaml``` config file:

```
ssl:
  enable2way: true
  overrideDefault: true
  keystore:
    path: /keystore.jks
    password: password
  truststore:
    path: /truststore.jks
    password: password
```

To disable 2-way SSL, change the ```ssl.enable2way``` config property to ```false```.

When ```ssl.overrideDefault``` config property is set to ```true```, our 2-way SSL ```SSLContext``` object will be set as the default (calling ```SSLContext.setDefault()```).

Both the keystore and truststore paths accept classpath and filesystem paths. In this sample, both of them are using the classpath as the files are under ```src/main/resources```.

# Configuring the SSLContext
The ```com.kaviddiss.twowayssldemo.config.TwoWaySSLConfig``` class is responsible for setting up 2-way SSL.

It loads the keystore and the optional truststore (if ```ssl.truststore.path``` is empty, the code will trust all certs when doing SSL handshake with a third-party service).

# Creating keystore with self-signed cert
```
> keytool -genkey -keyalg RSA -alias selfsigned -keystore keystore.jks -storepass password -validity 360 -keysize 2048
```

For more details, see https://www.sslshopper.com/article-how-to-create-a-self-signed-certificate-using-java-keytool.html.

# Exporting SSL cert from Chrome
See https://www.wikihow.com/Export-Certificate-Public-Key-from-Chrome

# Portecle
[Portecle](https://sourceforge.net/projects/portecle/) is a handy open-source GUI tool to work with keystores.

You can use both the ```keytool``` or Portecle to create a truststore and add exported SSL certs to it.

#
You can find more Spring Boot tutorials from me on [GitHub](https://github.com/davidkiss) and at http://kaviddiss.com/.
