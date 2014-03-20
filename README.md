## Comment générer le jar ?
```bash
atlas-package
```
## Comment installer le plugins ?

1. Ajout du jar dans WEB-INF/lib (Plugin static)
2. Référencement de la classe d'authentification dans le fichier seraph-config.xml présent dans WEB-INF/classes :

```xml
	<authenticator class="com.orange.jira.login.ShibbolethAuthenticator"/>
```

3. Référencement de l'url absolue de déconnexion Shibboleth :

```xml
	<param-name>logout.url</param-name>
	<param-value>https://jira.dev/Shibboleth.sso/Logout</param-value>
```