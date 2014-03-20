## Comment générer le jar ?

- Se rendre à la racine du projet et lancer la commande suivante

```bash
atlas-package
```
## Comment installer le plugins ?

- Ajout du jar dans WEB-INF/lib (Plugin static)
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