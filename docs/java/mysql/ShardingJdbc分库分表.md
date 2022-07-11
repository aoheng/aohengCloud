# aoheng-shardingjdbc

参考snowalker-shardingjdbc分库分表实战

分片规则：user_id,order_id

分片策略：复合分片策略-ComplexKeysShardingAlgorithm

项目实战GitHub：[aoheng-shardingjdbc]([GitHub - aoheng/aoheng-shardingjdbc: shardingjdbc分库分表实战](https://github.com/aoheng/aoheng-shardingjdbc))



## 了解Sharding-JDBC的数据分片策略

Sharding-JDBC的分片策略包含了分片键和分片算法。由于分片算法与业务实现紧密相关，因此Sharding-JDBC没有提供内置的分片算法，而是通过分片策略将各种场景提炼出来，提供了高层级的抽象，通过提供接口让开发者自行实现分片算法。

以下内容引用自官方文档。[官方文档](https://shardingsphere.apache.org/document/legacy/3.x/document/cn/features/sharding/concept/sharding/)



## **四种分片算法**

1. 通过分片算法将数据分片，支持通过=、BETWEEN和IN分片。
   分片算法需要应用方开发者自行实现，可实现的灵活度非常高。
2. 目前提供4种分片算法。由于分片算法和业务实现紧密相关，
   因此并未提供内置分片算法，而是通过分片策略将各种场景提炼出来，
   提供更高层级的抽象，并提供接口让应用开发者自行实现分片算法。



### 精确分片算法–PreciseShardingAlgorithm

用于处理使用单一键作为分片键的=与IN进行分片的场景。需要配合StandardShardingStrategy使用。



### 范围分片算法–RangeShardingAlgorithm

用于处理使用单一键作为分片键的BETWEEN AND进行分片的场景。需要配合StandardShardingStrategy使用。



### 复合分片算法–ComplexKeysShardingAlgorithm

用于处理使用多键作为分片键进行分片的场景，包含多个分片键的逻辑较复杂，需要应用开发者自行处理其中的复杂度。需要配合ComplexShardingStrategy使用。

注 ： 我们在业务开发中，经常有根据用户id 查询某用户的记录列表，又有根据某个业务主键查询该用户的某记录的需求，这就需要用到复合分片算法。比如，订单表中，我们既需要查询某个userId的某时间段内的订单列表数据，又需要根据orderId查询某条订单数据。这里，orderId与userId就属于复合分片键。



### Hint分片算法–HintShardingAlgorithm

Hint分片指的是对于分片字段非SQL决定，而由其他外置条件决定的场景，可以通过使用SQL Hint灵活注入分片字段。

Hint分片策略是绕过SQL解析的，因此能够通过实现该算法来实现Sharding-JDBC不支持的语法限制。

用于处理使用Hint行分片的场景。需要配合HintShardingStrategy使用。





## 五种分片策略



### 标准分片策略–StandardShardingStrategy

提供对SQL语句中的=, IN和BETWEEN AND的分片操作支持。StandardShardingStrategy只支持单分片键，提供PreciseShardingAlgorithm和RangeShardingAlgorithm两个分片算法。PreciseShardingAlgorithm是必选的，用于处理=和IN的分片。RangeShardingAlgorithm是可选的，用于处理BETWEEN AND分片，如果不配置RangeShardingAlgorithm，SQL中的BETWEEN AND将按照全库路由处理。



### 复合分片策略–ComplexShardingStrategy

提供对SQL语句中的=, IN和BETWEEN AND的分片操作支持。ComplexShardingStrategy支持多分片键，由于多分片键之间的关系复杂，因此并未进行过多的封装，而是直接将分片键值组合以及分片操作符透传至分片算法，完全由应用开发者实现，提供最大的灵活度。

这里体现出框架设计者对设计原则的透彻理解，将变更点暴露给用户，将不变的封装在内部，明确的划分了抽象和实现的界限，这是值得我们学习的。



### 行表达式分片策略–InlineShardingStrategy

使用Groovy的表达式，提供对SQL语句中的=和IN的分片操作支持，只支持单分片键。对于简单的分片算法，可以通过简单的配置使用，从而避免繁琐的Java代码开发，如: tuser$->{u_id % 8} 表示t_user表根据u_id模8，而分成8张表，表名称为t_user_0到t_user_7。

对于快速体验Sharding-JDBC的魅力是很有意义的，但是这种方式对于复杂的业务支持程度就差一些，因此实际的业务开发中还是推荐使用复合分片策略–ComplexShardingStrategy。



### Hint分片策略–HintShardingStrategy

通过Hint而非SQL解析的方式分片的策略。



### 不分片策略–NoneShardingStrategy

该策略为不分片的策略。







[官方文档](https://shardingsphere.apache.org/document/legacy/3.x/document/cn/features/sharding/concept/sharding/)
