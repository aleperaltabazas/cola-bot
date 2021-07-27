Cola bot
===

<p align="center">
    <img src="https://user-images.githubusercontent.com/31170385/126589374-d145bf36-46f1-45c2-b9ab-1e31fa15b523.jpeg" />
</p>

Bot sencillo para manejar colas

```
!queue delete q - delete queue 'q' (if it exists)
!queue help     - show this text message
!queue join q   - join queue 'q' (if you're not already in it)
!queue leave q  - leave queue 'q' (if you're already in it)
!queue list     - show all existing queues along with users awaiting in each
!queue next q   - call the next user in queue 'q' in
!queue new q    - create new queue 'q'
!queue status q - show the state of queue 'q' along with users waiting in it
```

## FAQ:

* ¿Cómo lo levanto?

Necesitás Maven, Kotlin, y un token de discord. El token lo tenés que poner en `src/main/resources/sensitive.conf`:

```
discord.bot.token="mi-token"
```

Y después de eso `mvn clean package` para armar el jar, y lo corrés con `java -jar/cola-bit-with-dependencies.jar`. En
cualquier caso, si te da mucha paja hacer esto, podés simplemente agregar el bot que ya está corriendo.

* ¿Como bija lo agrego?

Creo que clickeando [acá](https://discord.com/api/oauth2/authorize?client_id=867430374115246101&permissions=0&scope=bot)
