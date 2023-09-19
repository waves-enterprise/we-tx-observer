### [we-tx-tracker-module](we-tx-tracker-module)

The module consists of submodules:
* **api** - contains abstractions available for overriding in client code;
* **core-spring** - provides default implementations for abstractions from the api module;
* **domain** - observer domain entity models;
* **jpa** - spring configurations and repositories for working with database;
* **read-starter** - spring starter for reading tracked transactions;
* **starter** - spring starter for we-tx-tracker includes configuration of all main beans for correct operation. It differs from read-starter in that it allows you to save transactions that need to be tracked;
#### Example of creation and use with spring boot
Adding dependency \
Gradle:
```
implementation("com.wavesenterprise:we-tx-tracker-starter:$version")
implementation("com.wavesenterprise:we-tx-tracker-read-starter:$version")
```
Maven:
```xml
<dependency>
    <groupId>com.wavesenterprise</groupId>
    <artifactId>we-tx-tracker-starter</artifactId>
    <version>${version}</version>
</dependency>
<dependency>
    <groupId>com.wavesenterprise</groupId>
    <artifactId>we-tx-tracker-read-starter</artifactId>
    <version>${version}</version>
</dependency>
```
_* Note:_ it is necessary that the NodeBlockingServiceFactory bean is present in the context. For more information, see the documentation of the we-node-client and we-sdk-spring (we-starter-node-client) projects \
Let's consider a simple example of the tracker configuration.
But first you need to consider all possible settings:

`enabled` \
_Default: true_

`findContractInNode` \
Allows  to take information on a contract from a node if there are no records in the database for the required contract. _Default: true_

`fixedDelay` \
Rate of checking and updating transactions in the pending status. _Default: 10s_

`lockAtLeast` \
The minimum time for which the method will be blocked for other instances (in milliseconds). _Default: 0_.

`lockAtMost` \
The maximum time for which the method will be blocked for other instances (in milliseconds). _Default: 10000_.

`trackedTxPageRequestLimit` \
Limit on the number of pending transactions per iteration. _Default: 100_

`timeout` \

`minContractTxErrorCount` \

`minContractTxFailureCount` \

`failOnRecoverableContractTxError` \

```yaml
tx-tracker:
  enabled: true
  findContractInNode: true
  fixedDelay: 10s
  lockAtLeast: 0
  lockAtMost: 100000
  trackedTxPageRequestLimit: 100
  timeout: 2H
  minContractTxErrorCount: 1
  minContractTxFailureCount: 1
  failOnRecoverableContractTxError: true
```
Kotlin:
```kotlin
@Service
class ExampleService(
    private val txService: TxService,
    private val txTracker: TxTracker,
) {

    fun example() {
        val tx = txService.broadcast(TestDataFactory.createContractTx()) // broadcasting of tx(103)
        txTracker.trackTx(tx = tx) // adding to tx to tracking
    }
}
```
Java:
```java
@Service
class ExampleService {
    
    @Autowired
    private TxTracker txTracker;

    @Autowired
    private TxService txService;
    
    void example() {
        Tx tx = txService.broadcast(TestDataFactory.createContractTx()); // broadcasting of tx(103)
        txTracker.trackTx(tx); // adding to tx to tracking
    }
}
```
#### Ways to view the tracked transaction ([TxTrackInfo](we-tx-tracker-module%2Fwe-tx-tracker-domain%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fwavesenterprise%2Fwe%2Ftx%2Ftracker%2Fdomain%2FTxTrackInfo.kt)):
1) Using the TxTrackInfoService from `read-starter`;

Kotlin:
```kotlin
@Service
class ExampleService(
    private val txTrackInfoService: TxTrackInfoService
) {

    fun example(txId: String): TxTrackInfo =
        txTrackInfoService.getById(id = txId) // getting TxTrackInfo model
}
```
Java:
```java
@Service
class ExampleService {
    
    @Autowired
    private TxTrackInfoService txTrackInfoService;

    TxTrackInfo example(String txId) {
        return txTrackInfoService.getById(txId); // getting TxTrackInfo model
    }
}
```
2) Using endpoints provided from read-starter [TxTrackInfoController](we-tx-tracker-module%2Fwe-tx-tracker-read-starter%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fwavesenterprise%2Fwe%2Ftx%2Ftracker%2Fread%2Fstarter%2Fweb%2FTxTrackInfoController.kt);

To get a single element:
```curl
curl --location --request GET 'http://localhost:8080/tx-track-info/{txId}'
```
To get the list:
```curl
curl --location --request GET 'http://localhost:8080/tx-track-info' \
--header 'Content-Type: application/json' \
--data '{
    "status": "SUCCESS",
    "userId": null,
    "contractId": null
}'
```
3) Using the `tx_tracker` schema in the database in the `tx_track_info` table;

## Links:
* [Waves Enterprise documentation](https://docs.wavesenterprise.com/ru/latest/)