# we-tx-observer

**we-tx-observer** provides a convenient way of handling and tracking transactions in the Waves Enterprise blockchain network. 
It uses a persistent queue to store transactions from WE blocks. 
The transactions are further passed to handlers configured in the application. 

_**Note:** Only [PostgreSQL](https://www.postgresql.org/) is supported as the underlying database._

## Modules.
### [we-tx-observer-module](we-tx-observer-module)
The module consists of submodules:
* **api** - contains abstractions to be used in client code;
* **core-spring** - provides default implementations for abstractions from the api module;
* **domain** - observer domain entity models;
* **jpa** - spring configurations and repositories for working with database;
* **starter** - spring starter for we-tx-observer includes configuration of all main beans for correct operation;

## Getting started
To start using we-tx-observer you should:
### Add the dependency to your project
Gradle:
```kotlin
implementation("com.wavesenterprise:we-tx-observer-starter:$version")
```
Maven:
```xml
<dependency>
    <groupId>com.wavesenterprise</groupId>
    <artifactId>we-tx-observer-starter</artifactId>
    <version>${version}</version>
</dependency>
```
### Provide the necessary beans 
#### NodeBlockingServiceFactory
we-tx-observer uses the we-node-client to interact with node, so it is necessary that the `NodeBlockingServiceFactory` bean is present in the context. 
For more information, see the documentation of the **we-node-client** and **we-sdk-spring** (**we-starter-node-client**) projects:
* [we-node-client](https://gitlab.web3tech.ru/development/we/java-sdk/we-node-client/-/blob/dev/README.md)
* [we-sdk-spring](https://gitlab.web3tech.ru/development/we/java-sdk/we-sdk-spring/-/blob/dev/README.md)

#### TxEnqueuePredicate
If you are interested only in handling only specific transactions you should provide a `TxEnqueuePredicate` bean. 
This is an interface with a single `isEnqueued(Tx)` method. It is designed to filter transactions that appear in the persistent queue for future handling.

_**Note:** You can omit adding this bean and filter transactions later in your handlers 
but in that case redundant transaction would be put to persistent queue leading to unnecessary INSERTS and DELETES to the DB._ 

An example of TxEnqueuePredicate:
Kotlin:
```kotlin
@Component
class TxEnqueuePredicateImpl(
    val txService: TxService,
    @Value("\${contracts.config.example.id}")
    val contractId: String,
) : TxEnqueuePredicate {

    // overridden method
    override fun isEnqueued(tx: Tx): Boolean = when (tx) {
        // filtering a policy transaction needs to be processed by its policy name
        is PolicyDataHashTx -> filterForPolicyDataHashTx(tx)
        // filtering transactions for a specific contract by id
        is ExecutedContractTx -> tx.contractId().base58ContractId == contractId.base58ContractId
        else -> false
    }

    private fun filterForPolicyDataHashTx(tx: PolicyDataHashTx): Boolean {
        val policyTx = txService.txInfo(tx.policyId.txId).get().tx as CreatePolicyTx
        return filterForPolicyTx(policyTx)
    }

    private fun filterForPolicyTx(policyTx: CreatePolicyTx) =
        policyTx.policyName.value.startsWith("POLICY_NAME")
}
```
Java:
```java
@Component
class TxEnqueuePredicateImpl implements TxEnqueuePredicate {

    @Autowired
    private TxService txService;
    @Value("${contracts.config.example.id}")
    private String contractId;

    // overridden method
    @Override
    public boolean isEnqueued(Tx tx) {
        return switch (tx) {
            // filtering a policy transaction needs to be processed by its policy name
            case tx instanceof PolicyDataHashTx -> filterForPolicyDataHashTx(tx);
            // filtering transactions for a specific contract by id
            case tx instanceof ExecutedContractTx -> tx.contractId().base58ContractId == contractId.base58ContractId;
        };
    }

    private boolean filterForPolicyDataHashTx(PolicyDataHashTx tx) {
        CreatePolicyTx policyTx = (CreatePolicyTx) txService.txInfo(tx.getPolicyId().getTxId()).get().tx;
        return filterForPolicyTx(policyTx);
    }

    private boolean filterForPolicyTx(CreatePolicyTx policyTx) {
        return policyTx.getPolicyName().getValue().startsWith("POLICY_NAME");
    }
}
```

### TxQueuePartitionResolver
Specifies the partition for the received transaction (`defaultPartitionId` by default).  
Used to efficiently distribute transactions across partitions.  
By default, all transactions will fall into one partition - `defaultPartitionId`.
Implementation example of PartitionResolver  
Kotlin:
```kotlin
@Component
class UserPartitionResolver(
    private val txService: TxService,
) : TxQueuePartitionResolver {

    // overridden method
    override fun resolvePartitionId(tx: Tx): String? = when (tx) {
        is PolicyDataHashTx -> resolveForPolicyDataHashTx(tx)
        is ExecutedContractTx -> resolveForContractTx(tx)
        else -> null
    }

    // Looks for the required key in the state and removes the mapping name. The remaining value will be the partition ID.
    // If it does not find it, then the partition for this transaction will be equal to defaultPartitionId
    private fun resolveForContractTx(executedContractTx: ExecutedContractTx): String? =
        executedContractTx.results.map { param -> param.key.value }
            .find { paramKey -> paramKey.startsWith("OBJECT_") }
            ?.removePrefix("OBJECT_")

    // Searches for the policy creation transaction to obtain its name.
    // If the prefix matches the one you are looking for, the remaining value will be the partition id.
    private fun resolveForPolicyDataHashTx(policyDataHashTx: PolicyDataHashTx): String? {
        val createPolicyTx = txService.txInfo(policyDataHashTx.policyId.txId).get().tx as CreatePolicyTx
        return createPolicyTx.policyName.value
            .takeIf { policyName -> policyName.startsWith("OBJECT_") }
            ?.removePrefix("OBJECT_")
    }
}
```
Java:
```java
@Component
class UserPartitionResolver implements TxQueuePartitionResolver {
    
    @Autowired
    private TxService txService;

    // overridden method
    @Override
    String resolvePartitionId(Tx tx) {
        return switch (tx) {
            case tx instanceof PolicyDataHashTx -> resolveForPolicyDataHashTx(tx);
            case tx instanceof ExecutedContractTx -> resolveForContractTx(tx);
        };
    }

    // Looks for the required key in the state and removes the mapping name. The remaining value will be the partition ID.
    // If it does not find it, then the partition for this transaction will be equal to defaultPartitionId
    private String resolveForContractTx(ExecutedContractTx executedContractTx) {
        List<String> list = executedContractTx.getResults().stream()
                .filter(it -> it.getKey().getValue().startsWith("OBJECT_"))
                .map(it -> it.getKey().getValue())
                .toList();
        if (!list.isEmpty()) {
            return list.get(0).replaceAll("OBJECT_", "");
        } else {
            return null;
        }
    }

    // Searches for the policy creation transaction to obtain its name.
    // If the prefix matches the one you are looking for, the remaining value will be the partition id.
    private String resolveForPolicyDataHashTx(PolicyDataHashTx policyDataHashTx) {
        CreatePolicyTx createPolicyTx =  (CreatePolicyTx) txService.txInfo(policyDataHashTx.getPolicyId.getTxId).get().getTx();
        String policyName = createPolicyTx.getPolicyName().getValue();
        if (policyName.startsWith("OBJECT_")) {
            return policyName.replaceAll("OBJECT_", "");
        } else {
            return null;
        }
    }
}
```

### TxObserverConfigurer
Configurer for basic observer components. Allows you to define all bean-components through one bean: 
- TxQueuePartitionResolver;
- TxEnqueuePredicate (default implementation is `com.wavesenterprise.sdk.tx.observer.starter.TxObserverEnablerConfig.TxEnqueuePredicateConfigurerImpl`);
- ObjectMapper;
- PrivateContentResolver;  

It can be setup using TxObserverConfigurerBuilder or Kotlin DSL.
#### Example
Kotlin:
```kotlin
@Bean
fun txObserverConfigurer(): TxObserverConfigurer =
    TxObserverConfigurerBuilder()
        .partitionResolver(customPartitionResolver)
        .privateContentResolver(customPrivateContentResolver)
        .predicate(customEnqueuePredicate)
        .types(txTypes)
        .types(TX_TYPE_3, TX_TYPE_4)
        .build()
```
KotlinDsl:
```kotlin
@Bean
fun txObserverConfigurer(): TxObserverConfigurer = observerConfigurer {
    partitionResolver = customPartitionResolver
    privateContentResolver = customPrivateContentResolver
    predicates {
        types(txTypes)
        types(TX_TYPE_3, TX_TYPE_4)
        predicate(customEnqueuePredicate)
    }
}
```
Java:
```java
@Bean
TxObserverConfigurer txObserverConfigurer() {
        return new TxObserverConfigurerBuilder()
            .partitionResolver(customPartitionResolver)
            .privateContentResolver(customPrivateContentResolver)
            .predicate(customEnqueuePredicate)
            .types(txTypes)
            .types(TX_TYPE_3,TX_TYPE_4)
            .build();
        }
```

### Implementing handlers using @TxListener
After identifying filters for transactions, you need to write code for the listener that these filtered transactions will fall into.

[KeyFilter](we-tx-observer-module%2Fwe-tx-observer-api%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fwavesenterprise%2Fwe%2Ftx%2Fobserver%2Fapi%2Fkey%2FKeyFilter.kt) - annotation filters the KeyEvent by the keys from the state.
It has two optional parameters for filtering:
* `keyRegexp` - the key is in the form of a regular expression;
* `keyPrefix` - the key in the form of a prefix or a full string.

[PolicyFilter](we-tx-observer-module%2Fwe-tx-observer-api%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fwavesenterprise%2Fwe%2Ftx%2Fobserver%2Fapi%2Fprivacy%2FPolicyFilter.kt) - annotation which filters privacy data by policy name.
It has two optional parameters for filtering:
* `nameRegexp` - the key is in the form of a regular expression;
* `namePrefix` - the key in the form of a prefix or a full string.

[MessageFilter](we-tx-observer-module%2Fwe-tx-observer-api%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fwavesenterprise%2Fwe%2Ftx%2Fobserver%2Fapi%2Fprivacy%2FMessageFilter.kt) - annotation which filters privacy data by comment field when sending `privacy/sendData` in json format.
It has three optional parameters for filtering:
* `metaKey` - the key in the form of a key of json;
* `metaKeyValue` - the key in the form of a value of json;
* `metaKeyValueRegExp` - the key in the form of a regular expression in json;


_**Note:** Message `meta` is derived from privacy data comment parsed as json (`SendDataRequest.info.comment`). 
If the comment can't be parsed as JSON than this filter will fail._

[MessageFilters](we-tx-observer-module%2Fwe-tx-observer-api%2Fsrc%2Fmain%2Fkotlin%2Fcom%2Fwavesenterprise%2Fwe%2Ftx%2Fobserver%2Fapi%2Fprivacy%2FMessageFilter.kt) - annotation containing an array of `MessageFilter`.
It has a single field:
* `filters` - array of `MessageFilter`.
#### Examples
Kotlin:
```kotlin
@Component
class ExampleListener {
    @TxListener
    fun keyEventMyContract(
        @KeyFilter(keyPrefix = "EXAMPLES_") keyEvent: KeyEvent<String>, // filter for state keys
    ) {
        // do something with the received data
    }
    
    @TxListener
    fun onPrivacyContainer(
        @PolicyFilter(namePrefix = "POLICY_NAME_") // filter by policy name
        @MessageFilter(metaKey = "key", metaKeyValue = "value") // additional filtering parameter by the comment field
        privateDataEvent: PrivateDataEvent<String>,
    ) {
        // payload from PrivateDataEvent parameterized
        // do something with the received private data
    }

    @TxListener
    fun onUpdatePolicyTx(
        updatePolicyTx: UpdatePolicyTx,
    ) {
        // transaction is filtered by type
        // do something with the received UpdatePolicyTx
    }
}
```

Java:
```java
@Component
class ExampleListener {
    @TxListener
    void keyEventMyContract(
        @KeyFilter(keyPrefix = "EXAMPLES_") KeyEvent<String> keyEvent // filter for state keys
    ) {
        // do something with the received data
    }
    
    @TxListener
    void onPrivacyContainer(
        @PolicyFilter(namePrefix = "POLICY_NAME_") // filter by policy name
        @MessageFilter(metaKey = "key", metaKeyValue = "value") // additional filtering parameter by the comment field
        PrivateDataEvent<String> privateDataEvent
    ) {
        // payload from PrivateDataEvent parameterized
        // do something with the received private data
    }

    @TxListener
    void onUpdatePolicyTx(UpdatePolicyTx updatePolicyTx) {
        // transaction is filtered by type
        // do something with the received UpdatePolicyTx
    }
}
```
Method `keyEventMyContract(KeyEvent<String> keyEvent)` will receive data from the 105 tx after it has been mined.
In this transaction the contract method `call(string)` is called and a value with the key `EXAMPLES_` is added to the state.
```json
{
  "tx": {
    "senderPublicKey": "4qJpHUz8Y6vV5N21JZgUxAB3nNk5GU19SQZbC44fhSNbWJAb3afkD6ARAYAvkHgnb8CJWkmTuVDuc2NpximYVcva",
    "fee": 0,
    "type": 104,
    "params": [
      {
        "type": "string",
        "value": "call",
        "key": "action"
      },
      {
        "type": "string",
        "value": "test",
        "key": "string"
      }
    ],
    "version": 4,
    "contractVersion": 1,
    "sender": "3NpkC1FSW9xNfmAMuhRSRArLgnfyGyEry7w",
    "feeAssetId": null,
    "proofs": [],
    "contractId": "Dgk1hR7xRnDT1KJreaXCVtZLrnd5LJ8uUYtoZyQrV1LJ",
    "id": "7GExnxgASNMrveEmpHFVAkiHmjbwRj4ediwbE6imJyCo",
    "timestamp": 1644263008599
  },
  "sender": "3QQAXZAnJ8ppqekcgkoLqVNJEvJ4D8kjbVK",
  "proofs": [],
  "fee": 0,
  "id": "HerQyfkH4ui2K6RbHdxG2tFGnjLjiK4RveMu6CXXpp6E",
  "type": 105,
  "version": 1,
  "results": [
    {
      "type": "string",
      "value": "{\"example\":\"value\"}",
      "key": "EXAMPLES_7GExnxgASNMrveEmpHFVAkiHmjbwRj4ediwbE6imJyCo"
    }
  ],
  "timestamp": 1644263019011,
  "height": 9198547
}
```
Method `onPrivacyContainer(PrivateDataEvent<String> privateDataEvent)` can handle private data which has been sent using `privacy/sendData`:
```json
{
  "sender": "3HYW75PpAeVukmbYo9PQ3mzSHdKUgEytUUz",
  "password": "apgJP9atQccdBPA",
  "policyId": "4gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaC",
  "info": {
    "filename":"Service contract #100/5.doc",
    "size": 2048,
    "timestamp": 1000000000,
    "author": "AIvanov@org.com",
    "comment": {
      "key": "value"
    }
  },
  "data": "TWFuIGlzIGRpc3Rpbmd1aXNoZWQsIG5vdCBvbmx5IGJ5IGhpcyByZWFzb24sIGJ1dCBieSB0aGlzIHNpbmd1bGFyIHBhc3Npb24gZnJvbSBvdGhlciBhbmltYWxzLCB3aGljaCBpcyBhIGx1c3Qgb2YgdGhlIG1pbmQsIHRoYXQgYnkgYSBwZXJzZXZlcmFuY2Ugb2YgZGVsaWdodCBpbiB0aGUgY29udGludWVkIGFuZCBpbmRlZmF0aWdhYmxlIGdlbmVyYXRpb24gb2Yga25vd2xlZGdlLCBleGNlZWRzIHRoZSBzaG9ydCB2ZWhlbWVuY2Ugb2YgYW55IGNhcm5hbCBwbGVhc3VyZS4=",
  "hash": "FRog42mnzTA292ukng6PHoEK9Mpx9GZNrEHecfvpwmta",
  "broadcast": false
}
```

### we-tx-observer configuration
we-tx-observer-starter comes with sane defaults that perform well in most deployments without additional tweaking. However if necessary, it has flexible settings:

`enabled`  
Parameter responsible for enabling the observer logic. _Default: true_

`queue-mode`  
Queue implementation (at the current moment only _JPA_ is available). _Default: JPA_

`fixed-delay`  
Delay for the job which extract transactions to the queue (syncs the blocks). _Default: 2000_

`block-size-window`  
Max sum for the size of blocks being read at once in DataUnits. Sum of the `BlockSize` parameters in the node's response. _Default: 2MB_

`activation-height`   
Height of the blockchain from which the blocks will be synced.
It is useful if there is no need to re-read the entire blockchain, e.g. when starting a new app on the existing network.
Usually is specified as the height of the block containing 103 transaction which creates the application's smart contract. _Default: 1_

`node-alias-for-height`  
Node alias (name) for writing in `block_height_info` table. _Default: node_.

`block-height-window`  
Step for the upper bound of block range requested. Is used only if sum of the size is less than `block-size-window`.

`sync-history`  
Property that enables using the `activation-height`. If set to false than blocks will be synced from
the node's current block height. _Default: true_.

`block-history-depth`  
How much previous blocks should be saved to persistent buffer for resolving of potential fork.

`fork-not-resolved-height-drop`  
How many blocks to go back in case no common block found in history when handling a fork.

`block-history-clean-delay`  
Time (in millisecond) after which the `block_history` table will be cleaned. _Default: 1800000_

`liquid-block-polling-delay`  
Delay (in milliseconds) to wait for the mining of a key block. _Default: 200_

`auto-reset-height`  
Checking the height in the background in the table on block_height and on the node.
If the height in the table is greater, it will be reset to the height of the node (provided enabled). _Default: false_.

`errorPriorityOffset`  
Priority offset for partitions after which transactions with the NEW status can become POSTPONED from `EnqueuedTxController.postponeErrors()`.
_Default: 100_

`defaultPartitionId`  
Default String Id value for partition. This is set as `TxQueuePartition.id` if no bean of type `TxQueuePartitionResolver` is provided or its resolve method returns `null`. _Default: defaultPartitionId_

`lockEnabled`  
Enables [ShedLock](https://github.com/lukas-krecan/ShedLock|) for having only a single instance per Job when running in a multi-tenant environment.
The implementation used is `net.javacrumbs.shedlock.provider.jdbctemplate.JdbcTemplateLockProvider`. _Default: true_

`lockAtLeast`  
The minimum time for which the method will be blocked for other instances (in milliseconds) (This is a Schedlock specific property). _Default: 0_.

`lockAtMost`  
The maximum time for which the method will be blocked for other instances (in milliseconds) (This is a Schedlock specific property). _Default: 10000_.

##### queue-cleaner
Settings for cleaning up the `enqueued_tx` table.

`enabled`  
_Default: true_.

`archive-height-window`  
Offset for the height of cleaned transactions. _Default: 100_.

`delete-batch-size`  
Batch size used when cleaning the table. _Default: 100_.

`clean-cron-expression`  
Cron expression to configure the cleaning job. _Default: 0 0/5 * * * ?_ (EVERY 5 MINUTES)

##### privacy-check
Settings for the job that checks privacy availability.

`enabled`  
_Default: true_.

`fixed-delay`  
Rate for checking (in milliseconds). _Default: 500ms_.

`thread-count`  
Quantity of threads to perform checks. _Default: 3_

`limit-for-old`  
Limit for old EnqueuedTx records in the checked batch. _Default: 25_

`limit-for-recent`  
Limit for recent EnqueuedTx records in the checked batch. _Default: 10_

An example of the observer configuration in application.yml looks like this:
```yaml
tx-observer:
  enabled: true 
  queueMode: JPA 
  fixedDelay: 2000 
  blockSizeWindow: 10MB 
  activation-height: 1
  blockHeightWindow: 99
  syncHistory: true
  blockHistoryDepth: 100
  forkNotResolvedHeightDrop: 10
  blockHistoryCleanDelay: 1800000 
  liquidBlockPollingDelay: 200 
  autoResetHeight: false 
  errorPriorityOffset: 10
  defaultPartitionId: defaultPartitionId
  lockEnabled: true
  lockAtLeast: 0
  lockAtMost: 10000
  queue-cleaner:
    enabled: true
    archiveHeightWindow: 100
    deleteBatchSize: 100
    cleanCronExpression: 0 0/5 * * * ?
    lockAtLeast: 0
    lockAtMost: 10000
  privacy-check: 
    enabled: true
    fixedDelay: 500ms
    threadCount: 3
    limitForOld: 25
    limitForRecent: 10
```
If you don't specify anything than the default values will be used.

## Links:
* [Waves Enterprise documentation](https://docs.wavesenterprise.com/ru/latest/)


