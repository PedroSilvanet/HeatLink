# HeatLink

HeatLink é o módulo Wear OS do **Wave Legacy**. Envia informação simples de um heat de surf/bodyboard de um telemóvel Android para um relógio Wear OS emparelhado.

## Funcionalidades da versão 0.1

- envio manual de posição, última nota, nota necessária, prioridade e tempo;
- sincronização fiável através da Wear OS Data Layer;
- receção em segundo plano no relógio;
- vibração sempre que chega uma atualização;
- ecrã de alto contraste pensado para leitura rápida;
- integração direta com links Event Live do Wave Legacy, consultados a cada 3 segundos;
- compatibilidade adicional com links de eventos SurfScores;
- parser JSON normalizado e extensível para plataformas de resultados reais.

## Arquitetura

- `mobile`: painel Android e sincronização automática;
- `wear`: aplicação Wear OS e serviço de receção;
- `shared`: modelo, protocolo e parser partilhados.

O relógio deve estar emparelhado com o telefone. A Wear OS Data Layer escolhe automaticamente Bluetooth, Wi-Fi ou cloud, consoante as ligações disponíveis nos dispositivos.

## Wave Legacy automático

Cole no painel o link público do evento Wave Legacy, por exemplo:

```text
https://exemplo.pt/wavelegacy/event_live.php?id=42
```

A aplicação converte-o automaticamente no endpoint `event_state.php?id=42`, seleciona o heat ativo e acompanha o atleta indicado. Não é necessária qualquer alteração manual aos resultados: notas inseridas pelos juízes, prioridade e progressão já chegam através do estado público do Wave Legacy.

## SurfScores automático

Cole no painel o link normal do evento, por exemplo:

```text
https://www.surfscores.com/?league=180&event=600
```

O HeatLink descobre o identificador público do placar, lê o heat ativo e seleciona o atleta pelo nome. Sempre que os dados mudam, envia a atualização para o relógio.

## Feed JSON alternativo

Nesta primeira versão, o URL deve devolver JSON no formato abaixo. Também são reconhecidos alguns nomes alternativos, como `rank`, `latestScore` e `scoreToAdvance`.

```json
{
  "heat": {
    "name": "Final Open",
    "timeRemaining": "04:32",
    "athletes": [
      {
        "name": "Nome do atleta",
        "position": 2,
        "lastScore": 5.40,
        "scoreNeeded": 6.25,
        "priority": 1
      }
    ]
  }
}
```

Outras plataformas podem ser acrescentadas através de um adaptador em `mobile/LiveHeatSource.kt`.

## Compilar

Requisitos:

- Android Studio com JDK 17;
- Android SDK 36;
- um telefone Android com Google Play Services;
- relógio Wear OS emparelhado.

Abra a pasta na versão atual do Android Studio, sincronize o Gradle e execute primeiro `mobile` no telefone e depois `wear` no relógio.

## Testar sem um site de resultados

1. Abra o painel no telefone.
2. Preencha os campos manualmente.
3. Toque em **ENVIAR AGORA**.
4. O relógio deve vibrar e atualizar o ecrã.

Para testar o modo automático, aponte o campo URL para um servidor que devolva o JSON de exemplo.

## Limitações

- A aplicação não transforma um relógio sem LTE em dispositivo de longo alcance. A comunicação depende das ligações suportadas pelo relógio e telefone.
- O modo automático precisa de um feed JSON ou de um adaptador específico para o site escolhido.
- A resistência à água depende exclusivamente do modelo de relógio utilizado.

## Licença

MIT
