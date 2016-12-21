# Remote User HTTP Authenticator

Ceci est un connecteur Jira 7.x permettant de déléguer l'authentification applicative au [SSO Shibboleth](https://shibboleth.net). 

## Comment générer le jar ?

- Se rendre à la racine du projet et lancer la commande suivante :

```bash
atlas-package
```
- L'archive générée se trouvera dans target.

## Comment installer le plugins dans Jira ?

- Ajout du jar dans WEB-INF/lib (Plugin static).

- Référencement de la classe d'authentification dans le fichier seraph-config.xml présent dans WEB-INF/classes :

```xml
<authenticator class="com.orange.jira.login.ShibbolethAuthenticator"/>
```
- Référencement des url relatives de login de Shibboleth dans le fichier seraph-config.xml :

```xml
<param-name>login.url</param-name>
<param-value>/Shibboleth.sso/Login?target=${originalurl}</param-value>
```

```xml
<param-name>link.login.url</param-name>
<param-value>/Shibboleth.sso/Login?target=${originalurl}</param-value>
```

- Référencement de l'url absolue de déconnexion Shibboleth dans le fichier seraph-config.xml :

```xml
<param-name>logout.url</param-name>
<param-value>https://jira/Shibboleth.sso/Logout</param-value>
```

## Comment configurer Shibboleth, Apache et Tomcat ?

### Configuration du SP Shibboleth

- Pour que Shibboleth renseigne le REMOTE_USER dans le Request, il faut configurer son mapping de champ dans le fichier Shibbolet2.xml : 

```xml
<ApplicationDefaults ... REMOTE_USER="uid" ... >
```

- Pour rediriger l'utilisateur vers l'url de déconnexion Shibboleth, il faut changer le contenu des fichiers localLogout.html, partialLogout.html et globalLayout.html :

```html
<html>
        <head>
                <script type="text/javascript">
                         window.location="https://shibboleth/idp/decoidp.jsp?url=https://jira";
                </script>
        </head>
        <body>
        </body>
</html>
```

### Configuration du vhost Apache

- Voici les instructions à ajouter dans le fichier vhost de l'application : 

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

- Pour que Tomcat ne détruise pas la valeur du REMOTE_USER envoyée par Apache, il faut renseigner tomcatAuthentication dans la description du connecteur AJP du server_head.xml : 

```xml
<Connector port="8009" protocol="AJP/1.3" ... tomcatAuthentication="false" ... />
```
