package org.example;

import io.grpc.Server;
import io.grpc.ServerBuilder;
import io.grpc.stub.StreamObserver;
import numbers.Numbers;
import numbers.NumbersServiceGrpc;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class NumbersServer {

    // Инициализация логгера для логирования событий сервера
    private static final Logger logger = Logger.getLogger(NumbersServer.class.getName());

    // Порт, на котором будет запущен сервер
    private final int port;
    // Экземпляр сервера gRPC
    private final Server server;

    // Конструктор класса, принимающий порт для запуска сервера
    public NumbersServer(int port) {
        this.port = port;
        // Создание сервера gRPC на заданном порту и добавление реализации сервиса
        this.server = ServerBuilder.forPort(port)
                .addService(new NumbersServiceImpl())
                .build();
    }

    // Метод для запуска сервера
    public void start() throws IOException {
        server.start();
        // Логирование информации о запуске сервера
        logger.info("Server started, listening on " + port);
        // Добавление хука для завершения работы сервера при завершении работы JVM
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                System.err.println("*** shutting down gRPC server since JVM is shutting down");
                try {
                    NumbersServer.this.stop();
                } catch (InterruptedException e) {
                    e.printStackTrace(System.err);
                }
                System.err.println("*** server shut down");
            }
        });
    }

    // Метод для остановки сервера
    public void stop() throws InterruptedException {
        if (server != null) {
            server.shutdown().awaitTermination(30, TimeUnit.SECONDS);
        }
    }

    // Метод для блокировки основного потока до тех пор, пока сервер не будет остановлен
    private void blockUntilShutdown() throws InterruptedException {
        if (server != null) {
            server.awaitTermination();
        }
    }

    // Точка входа в приложение
    public static void main(String[] args) throws IOException, InterruptedException {
        // Создание экземпляра сервера с портом 8980
        NumbersServer server = new NumbersServer(8980);
        // Запуск сервера
        server.start();
        // Блокировка основного потока до тех пор, пока сервер не будет остановлен
        server.blockUntilShutdown();
    }

    // Реализация сервиса NumbersService
    static class NumbersServiceImpl extends NumbersServiceGrpc.NumbersServiceImplBase {
        // Метод для обработки запроса на получение последовательности чисел
        @Override
        public void getNumberSequence(Numbers.NumberRequest request, StreamObserver<Numbers.NumberResponse> responseObserver) {
            int firstValue = request.getFirstValue();
            int lastValue = request.getLastValue();

            // Генерация последовательности чисел с задержкой в 2 секунды между каждым числом
            for (int i = firstValue; i <= lastValue; i++) {
                try {
                    TimeUnit.SECONDS.sleep(2);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                // Создание ответа с текущим числом
                Numbers.NumberResponse response = Numbers.NumberResponse.newBuilder().setValue(i).build();
                // Отправка ответа клиенту
                responseObserver.onNext(response);
            }
            // Уведомление клиента о завершении потока ответов
            responseObserver.onCompleted();
        }
    }
}
