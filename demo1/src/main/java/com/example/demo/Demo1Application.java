package com.example.demo;

import io.confluent.kafka.serializers.KafkaAvroSerializer;
import org.apache.kafka.clients.producer.Callback;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import java.time.Instant;
import java.util.Properties;

@SpringBootApplication
public class Demo1Application {

    public static void main(String[] args) {
        Properties properties = new Properties();

        properties.put("bootstrap.servers", "localhost:9092");
        properties.put("acks", "all");
        properties.put("retries", "10");
        properties.put("key.serializer", StringSerializer.class.getName());
        properties.put("value.serializer", KafkaAvroSerializer.class.getName());
        properties.put("schema.registry.url", "http://localhost:8085");
        KafkaProducer<String,TransactionEventAvro> kafkaProducer = new KafkaProducer<String,TransactionEventAvro>(properties);

        String topic = "transactions";
        TransactionEventAvro transactionEventAvro = TransactionEventAvro.newBuilder()
                .setTransactionID("az45esq5d46az5e4qs2d65azea")
                .setAccountNumber("aze321aze12")
                .setAtmAddress("16 rue tripoli la goulette")
                .setAtmID("atmtnlg148354782541")
                .setCustomerID("cst65432165432654")
                .setTimestamp(Instant.now())
                .setTransactionAmount(222.5)
                .setTransactionType("withdraw")
                .build();
        ProducerRecord<String,TransactionEventAvro> producerRecord = new ProducerRecord<String,TransactionEventAvro>(topic,transactionEventAvro);
        System.out.println("[ PRODUCER ] - "+transactionEventAvro);
        kafkaProducer.send(producerRecord, new Callback() {
            @Override
            public void onCompletion(RecordMetadata recordMetadata, Exception exception) {
                if (exception == null) {
                    System.out.println(" [BROKER] - Successfully received the details as: \n" +
                            "Topic: " + recordMetadata.topic() + "\n" +
                            "Partition: " + recordMetadata.partition() + "\n" +
                            "Offset: " + recordMetadata.offset() + "\n" +
                            "Timestamp: " + recordMetadata.timestamp()+ "\n");
                } else {
                    exception.printStackTrace();
                }
            }
        });

        kafkaProducer.flush();
        kafkaProducer.close();
    }

}
