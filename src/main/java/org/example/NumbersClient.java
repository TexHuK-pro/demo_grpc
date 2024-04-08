package org.example;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;
import numbers.Numbers;
import numbers.NumbersServiceGrpc;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class NumbersClient {
    // Инициализация логгера для логирования событий клиента
    private static final Logger logger = Logger.getLogger(NumbersClient.class.getName());

    // Канал для связи с сервером gRPC
    private final ManagedChannel channel;

    // Асинхронный шлюз для вызова удаленных процедур
    private final NumbersServiceGrpc.NumbersServiceStub asyncStub;

    // Конструктор класса, принимающий хост и порт сервера
    public NumbersClient(String host, int port) {
        // Создание канала связи с сервером
        this.channel = ManagedChannelBuilder.forAddress(host, port)
                .usePlaintext()
                .build();
        // Создание асинхронного шлюза для вызова удаленных процедур
        this.asyncStub = NumbersServiceGrpc.newStub(channel);
    }

    // Метод для завершения работы канала связи
    public void shutdown() throws InterruptedException {
        channel.shutdown().awaitTermination(5, TimeUnit.SECONDS);
    }

    // Метод для запроса последовательности чисел у сервера
    public void getNumberSequence(int firstValue, int lastValue) {
        // Создание запроса с начальным и конечным значениями
        Numbers.NumberRequest request = Numbers.NumberRequest.newBuilder()
                .setFirstValue(firstValue)
                .setLastValue(lastValue)
                .build();

        // Обработчик ответов от сервера
        StreamObserver<Numbers.NumberResponse> responseObserver = new StreamObserver<Numbers.NumberResponse>() {
            private int currentValue = 0;

            // Метод, вызываемый при получении нового ответа от сервера
            @Override
            public void onNext(Numbers.NumberResponse value) {
                // Обновление текущего значения на основе полученного числа
                currentValue = currentValue + value.getValue() + 1;
                // Логирование текущего значения
                logger.info("currentValue:" + currentValue);
            }

            // Метод, вызываемый при возникновении ошибки при получении ответа от сервера
            @Override
            public void onError(Throwable t) {
                logger.severe("Error: " + t.getMessage());
            }

            // Метод, вызываемый после получения всех ответов от сервера
            @Override
            public void onCompleted() {
                logger.info("Sequence completed");
            }
        };

        // Вызов удаленной процедуры getNumberSequence на сервере
        asyncStub.getNumberSequence(request, responseObserver);
    }

    // Точка входа в приложение
    public static void main(String[] args) throws InterruptedException {
        // Создание экземпляра клиента, подключающегося к серверу на localhost:8980
        NumbersClient client = new NumbersClient("localhost", 8980);
        try {
            // Запрос последовательности чисел у сервера каждую десятюю секунду
            for (int i = 0; i <= 50; i++) {
                if (i % 10 == 0) {
                    client.getNumberSequence(0, 30);
                }
                TimeUnit.SECONDS.sleep(1);
            }
        } finally {
            // Завершение работы клиента и канала связи
            client.shutdown();
        }
    }
}
