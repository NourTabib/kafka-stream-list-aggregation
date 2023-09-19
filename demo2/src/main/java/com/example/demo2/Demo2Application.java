package com.example.demo2;

import io.confluent.kafka.serializers.AbstractKafkaSchemaSerDeConfig;
import io.confluent.kafka.serializers.KafkaAvroDeserializerConfig;
import io.confluent.kafka.serializers.KafkaAvroSerializer;
import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerde;
    import io.confluent.kafka.streams.serdes.avro.SpecificAvroSerializer;
import org.apache.avro.generic.GenericData;
import org.apache.avro.specific.SpecificRecord;
import org.apache.kafka.clients.producer.*;
import org.apache.kafka.common.serialization.Serde;
import org.apache.kafka.common.serialization.Serdes;
import org.apache.kafka.common.serialization.StringSerializer;
import org.apache.kafka.common.utils.Bytes;
import org.apache.kafka.streams.KafkaStreams;
import org.apache.kafka.streams.KeyValue;
import org.apache.kafka.streams.StreamsBuilder;
import org.apache.kafka.streams.StreamsConfig;
import org.apache.kafka.streams.kstream.*;
import org.apache.kafka.streams.state.KeyValueStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.stereotype.Component;
import sun.applet.Main;

import java.time.Instant;
import java.util.*;
import java.util.logging.Logger;

@SpringBootApplication
@EnableKafka
@EnableKafkaStreams
public class Demo2Application {



    public static void main(String[] args) {
        Thread thread = new Thread(Demo2Application::Produce);
        thread.start();
        System.out.println("Main thread is executing.");
        Properties properties1 = new Properties();
        properties1.put(StreamsConfig.APPLICATION_ID_CONFIG, "streams-test-8");
        properties1.put(StreamsConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        properties1.put(StreamsConfig.DEFAULT_KEY_SERDE_CLASS_CONFIG, Serdes.String().getClass().getName());
        properties1.put(StreamsConfig.NUM_STREAM_THREADS_CONFIG,4);
        properties1.put(StreamsConfig.DEFAULT_VALUE_SERDE_CLASS_CONFIG, SpecificAvroSerde.class);
        properties1.put("transaction.state.log.replication.factor",1);
        properties1.put("schema.registry.url", "http://localhost:8081");
        final Serde<TransactionEventAvro> TRANSACTION_SERDE = new SpecificAvroSerde<>();
        final Serde<List<TransactionEventAvro>> LISTE_SERDE = new Serdes.ListSerde(ArrayList.class,TRANSACTION_SERDE);
        LISTE_SERDE.configure(Collections.singletonMap("schema.registry.url",
                "http://localhost:8081"),false);
        final Map<String, String> serdeConfig = Collections.singletonMap("schema.registry.url",
                "http://localhost:8081");
        TRANSACTION_SERDE.configure(serdeConfig, false);
        StreamsBuilder builder = new StreamsBuilder();
        KStream<String, TransactionEventAvro> stream = builder.stream("transactions.events",Consumed.with(Serdes.String(),TRANSACTION_SERDE));
        stream.peek((key, value) -> {

        });
        KGroupedStream<String,TransactionEventAvro> eventAvroKGroupedStream = stream
                .map((key,value)-> new KeyValue<>(value.getAccountNumber().toString(),value))
                .groupBy(
                        (key,value) -> key ,
                        Grouped.with(Serdes.String(),TRANSACTION_SERDE)
                );
        KTable<String,List<TransactionEventAvro>> kTable = eventAvroKGroupedStream
                .aggregate(
                        ArrayList<TransactionEventAvro>::new,
                        (key,value,list) -> {
                            list.add(value);
                            return list;
                        },
                        Materialized.<String,List<TransactionEventAvro>,KeyValueStore<Bytes,byte[]>>as("GROUPED-KTABLE-EVENTS-STORE-2")
                                .withKeySerde(Serdes.String())
                                .withValueSerde(LISTE_SERDE)
                                //with(Serdes.String(),LISTE_SERDE)
                );
        kTable.toStream()
                //.peek((key,value) -> System.out.println("GROUPED STREAM PEEK || " +key + " : " + value.get(0).getAtmAddress() + value.get(1).getAtmAddress() + "\n"))
                .to("transactions.events.grouped",Produced.with(Serdes.String(),LISTE_SERDE));

        builder.stream("transactions.events.grouped",Consumed.with(Serdes.String(),LISTE_SERDE))
                .map((key,value)-> new KeyValue<>(key,value.toString()))
                .peek((key,value) -> System.out.println("Key : "+ key + " Value: " + value))
                .to("transactions.events.output",Produced.with(Serdes.String(),Serdes.String()));
        KafkaStreams kafkaStreams = new KafkaStreams(builder.build(),properties1);
        kafkaStreams.start();
        Runtime.getRuntime().addShutdownHook(new Thread(kafkaStreams::close));
    }
    public static void Produce(){
        List<String> addressList = Arrays.asList(
                "Rue de Rome, Tunis",
                "Avenue Habib Bourguiba, Tunis",
                "Rue du Lac Malaren, Tunis",
                "Rue de Marseille, Tunis",
                "Avenue Mohamed V, Tunis",
                "Rue Charles de Gaulle, Tunis",
                "Rue Ibn Khaldoun, Tunis",
                "Avenue de Paris, Tunis",
                "Rue du Pacha, Tunis",
                "Rue de l'Indépendance, Tunis",
                "Avenue Farhat Hached, Tunis",
                "Rue de Kairouan, Tunis",
                "Avenue de Carthage, Tunis",
                "Rue du Maroc, Tunis",
                "Rue de l'Algérie, Tunis",
                "Avenue de la République, Tunis",
                "Rue Ibn Sina, Tunis",
                "Rue de Sousse, Tunis",
                "Avenue Habib Thameur, Tunis",
                "Rue d'Italie, Tunis",
                "Rue de Tripoli, Tunis",
                "Avenue Mohamed Ali, Tunis",
                "Rue d'Angleterre, Tunis",
                "Rue de Belgique, Tunis",
                "Avenue Habib Bourguiba, Tunis",
                "Rue d'Allemagne, Tunis",
                "Avenue Habib Bourguiba, Tunis",
                "Rue du Maréchal Tito, Tunis",
                "Rue de la Libye, Tunis",
                "Avenue de la Ligue Arabe, Tunis"
        );
        List<String> atmIdList = new ArrayList<>();
        for (int i = 0; i < addressList.size(); i++) {
            atmIdList.add(UUID.randomUUID().toString());
        }
        List<String> operations = Arrays.asList("withdraw", "deposit", "check");
        List<String> accountNumbers = new ArrayList<>();
        for (int i = 0; i < 6000; i++) {
            accountNumbers.add(UUID.randomUUID().toString());
        }
        List<String> customerIdList = new ArrayList<>();
        for (int i = 0; i < 6000; i++) {
            customerIdList.add(UUID.randomUUID().toString());
        }
        Properties properties = new Properties();
        properties.put(ProducerConfig.CLIENT_ID_CONFIG, "producer-test");
        properties.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:29092");
        properties.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());

        properties.put("transaction.state.log.replication.factor",1);
        properties.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, KafkaAvroSerializer.class.getName());
        properties.put("schema.registry.url", "http://localhost:8081");
        KafkaProducer<String, TransactionEventAvro> kafkaProducer = new KafkaProducer<String, TransactionEventAvro>(properties);

        String topic = "transactions.events";
        Random random = new Random();
        Instant startTime = Instant.now();
        Instant endTime = startTime.plusSeconds(60*20   );
        while (true) {
            int i = 50;
            while (true) {
                int AccCusNum = random.nextInt(customerIdList.size());
                int AtmNum = random.nextInt(atmIdList.size());
                double Amount = 10 + (1000 - 10) * random.nextDouble();

                TransactionEventAvro transactionEventAvro = TransactionEventAvro.newBuilder()
                        .setTransactionID(UUID.randomUUID().toString())
                        .setAccountNumber(accountNumbers.get(AccCusNum))
                        .setAtmAddress(addressList.get(AtmNum))
                        .setAtmID(atmIdList.get(AtmNum))
                        .setCustomerID(customerIdList.get(AccCusNum))
                        .setTimestamp(Instant.now())
                        .setTransactionAmount(Amount)
                        .setTransactionType(operations.get(random.nextInt(3)))
                        .build();
                ProducerRecord<String, TransactionEventAvro> producerRecord = new ProducerRecord<String, TransactionEventAvro>(topic, transactionEventAvro.getTransactionID().toString(), transactionEventAvro);
                kafkaProducer.send(producerRecord, new Callback() {
                    @Override
                    public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
                        if (exception == null) {
                            System.out.println(" [EVENT] - " +
                                    "Topic: " + recordMetadata.topic() + "\t ," +
                                    "Partition: " + recordMetadata.partition() + "\t ," +
                                    "Offset: " + recordMetadata.offset() + "\t ," +
                                    "Timestamp: " + recordMetadata.timestamp() + "\t ,");
                        } else {
                            exception.printStackTrace();
                        }
                    }
                });
                kafkaProducer.flush();
                if (i == 0) {
                    i = 50;
                    break;
                } else {
                    i--;
                }
            }
            if (Instant.now().isAfter(endTime)) {
                break;
            }
        }
    }
}
