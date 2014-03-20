# Jira Shibboleth Connector

Ceci est un connecteur Jira permettant de déléguer l'autentification applicative au [SSO Shibboleth](https://shibboleth.net). 

## Comment générer le jar ?

- Se rendre à la racine du projet et lancer la commande suivante :

```bash
atlas-package
```
- L'archive générée sera dans target.

## Comment installer le plugins dans Jira ?

- Ajout du jar dans WEB-INF/lib (Plugin static).

- Référencement de la classe d'authentification dans le fichier seraph-config.xml présent dans WEB-INF/classes :

```xml
<authenticator class="com.orange.jira.login.ShibbolethAuthenticator"/>
```
- Référencement des url relatives de login de Shibboleth :

```xml
<param-name>login.url</param-name>
<param-value>/Shibboleth.sso/Login?target=${originalurl}</param-value>
```

```xml
<param-name>link.login.url</param-name>
<param-value>/Shibboleth.sso/Login?target=${originalurl}</param-value>
```

- Référencement de l'url absolue de déconnexion Shibboleth :

```xml
<param-name>logout.url</param-name>
<param-value>https://jira.dev/Shibboleth.sso/Logout</param-value>
```

## Comment configurer Shibboleth / Apache / Tomcat ?

### Configuration du SP Shibboleth

- Pour que Shibboleth renseigne le REMOTE_USER dans le Request, il faut rensigner son mapping de champ dans le fichier **Shibbolet2.xml** : 

```xml
<ApplicationDefaults ... REMOTE_USER="uid" ... >
```

- Pour rediriger l'utilisateur vers la déconnexion Shibboleth, il faut changer le contenu des fichiers **localLogout.html**, **partialLogout.html** et **globalLayout.html** :

```html
<html>
        <head>
                <script type="text/javascript">
                         window.location="https://sso-itg.si.mbs/idp/decoidp.jsp?url=https://jira.dev";
                </script>
        </head>
        <body>
        </body>
</html>
```

### Configuration du vhost Apache

- Pour que Shibboleth renseigne le REMOTE_USER en Request, il faut lui donner le bon mapping dans le fichier Shibbolet2.xml : 

```xml
ProxyRequests         Off
ProxyPreserveHost     On
ProxyPass             /           ajp://localhost:8009/
ProxyPassReverse      /           ajp://localhost:8009/

<Location />
        AuthType shibboleth
        require shibboleth
</Location>

<Proxy *>
        Order deny,allow
        Allow from localhost
</Proxy>
```

### Configuration du connector Tomcat

- Pour que Tomcat ne détruise la valeur du REMOTE_USER envoyée par Apache, il faut modifier de renseigner l'attribut tomcatAuthentication dans le fichier server_head.xml : 

```xml
<Connector port="8009"
	maxThreads="800" connectionTimeout="60000" minSpareThreads="25" maxSpareThreads="75"
    enableLookups="false" redirectPort="8443" protocol="AJP/1.3" URIEncoding="UTF-8" tomcatAuthentication="false" />
```