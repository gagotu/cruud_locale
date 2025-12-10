# CRUUD

## Descrizione
L’obiettivo del sistema software CRUUD (Cruise-UD Transformer) è quello di convertire in appositi file con 
struttura Urban Dataset OWL (in seguito UD OWL), i diversi file csv/json “non strutturati” seguendo 
determinati parametri di estrazione impostati dall’utente. 


## Installazione ed esecuzione del progetto in ambiente locale
#### Prerequisiti
- [ ] Installazione di Docker
- [ ] Linea di commandi, cmd/poweshell per sistema operativo Windows e Shell per sistemi operativi Linux


#### Installazione
- [ ] Installare Docker da [qui](https://www.docker.com/products/docker-desktop/).


#### Esecuzione
Spostarsi nella cartella /docker/ ed eseguire il file:

  > build-image.bat

successivamente entrare in /docker/compose/ e lanciare il comando:

  > docker-compose up -d

Se l'installazione finisce con successo sarà possibile ragiungere l'applicativo 
da questo [link](http://localhost:8090).

Per chiudere l'esecuzione dei container appena creati, lavorare dal docker manager oppure lanciare il comando:

  > docker-compose stop

#### Comandi utili 
> __*docker-compose up --build <service_name oppure vuoto>  --build --force-recreate --remove-orphans *__  - Commando per avviare Docker pulendo la cache per riflettere i cambiamenti in configurazione
>
> __*docker-compose stop*__  - Commando per stoppare i container
> 
> __*docker-compose start*__  - Commando per riavviare i container
> 
> __*docker-compose down*__  - Commando per terminare Docker e rimuovere tutti i container creati
> 
> __*docker build --no-cache --progress=plain -t geco .\gecoregistration*__  - Commando per eseguire un specifico servizio visualizzando i log 