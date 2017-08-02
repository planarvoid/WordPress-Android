# Consumer Driven Contracts for API Mobile

For [consumer driven contract testing](https://martinfowler.com/articles/consumerDrivenContracts.html) in `api-mobile` we use the [Pact](https://docs.pact.io/) framework.

[api-mobile-cdc](https://ci.dev.s-cloud.net/go/tab/pipeline/history/api-mobile-cdc) verifies all the [pacts](https://docs.pact.io/documentation/how_does_pact_work.html) published by consumers of `api-mobile`. These pacts are pulled from our central [pact broker](http://pact-broker.dev.s-cloud.net/).

This `pacts` folder lists all the interactions(`*_interaction.json` files) that will be integrated as part of a single pact file(`android-api-mobile.json`) which will be published to the pact broker.

There are scripts to generate(`generate.sh`) and publish(`publish.sh`) these pacts, which will run automatically on CI.

### Defining a new interaction

* Create a new interaction file(eg. `profile_info_interaction.json`).
* Update file with the contract `api-mobile` should honor(refer existing interaction files for example).
* Test the interaction against `api-mobile`(details below).
* Commit and push the interaction file.

### Testing the interactions

* `pacts/generate.sh` to generate the pact file(`pacts/android-api-mobile.json`) for `api-mobile`.
* Copy `pacts/android-api-mobile.json` to `pacts` folder under `api-mobile` root directory. Create the folder if it does not already exist.
* Go to the `api-mobile` root directory.
* Run `make pact-verify` (you will need VPN, [cd-tools](https://github.com/soundcloud/cd-tools) and `docker` for this to work).

Note: The generated pact file contains 'all' interactions defined by the files(`*_interaction.json`) in this folder. So `pact-verify` will verify all the interactions.

