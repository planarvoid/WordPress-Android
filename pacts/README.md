# Consumer Driven Contracts for API Mobile

For [consumer driven contract testing](https://martinfowler.com/articles/consumerDrivenContracts.html) in `api-mobile` we use the [Pact](https://docs.pact.io/) framework.

The [api-mobile-cdc](https://ci.dev.s-cloud.net/go/tab/pipeline/history/api-mobile-cdc) pipeline verifies all the [pacts](https://docs.pact.io/documentation/how_does_pact_work.html) published by consumers of `api-mobile`. These pacts are pulled from our central [pact broker](http://pact-broker.dev.s-cloud.net/).

A pact between a consumer(like `android`) and a producer(like `api-mobile`) consists of multiple interactions. This `pacts` folder lists all the interactions(`*.json` files in the `interactions` folder) that make up the pact between `android` and `api-mobile`.

The [android-api-mobile-pact](https://ci.dev.s-cloud.net/go/tab/pipeline/history/android-api-mobile-pact) pipeline is responsible for generating and publishing the pact for Android. Note that a new interaction will only be published if it succeeds.

### Defining a new interaction

* Create a new interaction file in the `pacts/interactions` folder (eg. `profile_info_interaction.json`).
* Update file with the contract `api-mobile` should honor(refer existing interaction files for example).
* Test the interaction against `api-mobile`(details below).
* Commit and push the interaction file.

### Testing the interactions

* `pacts/generate.sh` to generate the pact file(`pacts/android-api-mobile.json`).
* Copy `pacts/android-api-mobile.json` to the `pacts` folder under `api-mobile`'s root directory. Create the folder if it does not already exist.
* Go to the `api-mobile` root directory.
* Run `make pact-verify` (you will need VPN, [cd-tools](https://github.com/soundcloud/cd-tools) and `docker` for this to work).

Note: The generated pact file contains 'all' interactions defined by the files(`*_interaction.json`) in this folder. So `pact-verify` will verify all the interactions and not just the new interaction.

