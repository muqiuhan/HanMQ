import org.scalatest.funsuite.AnyFunSuite
import client.*
import java.net.URI
import server.Server

class TestClient extends AnyFunSuite:
  val url = "ws://localhost:9993/";

  test("Test producer") {
    val producer1 = new Producer(URI.create(url), "producer_1");
    val producer2 = new Producer(URI.create(url), "producer_2");
    val producer3 = new Producer(URI.create(url), "producer_3");

    producer1.send("Make America Great Again!", "American.great.again.!");
    producer2.send("China is getting stronger!", "China.daily.com");
    producer2.send("中国建党一百年万岁", "China.xinhua.net");
    producer3.send("The voice from Europe", "UK.Reuters.com");

    System.in.read()
  }

  test("test consumer") {
    val consumer1 = new Consumer(URI.create(url), "American");
    val consumer2 = new Consumer(URI.create(url), "China");
    val consumer3 = new Consumer(URI.create(url), "UK");

    consumer1.register("American", true);
    consumer1.onMessage(message => println("American: " + message));
    consumer2.register("China", true);
    consumer2.onMessage(message => println("China: " + message));
    consumer3.register("UK", true);
    consumer3.onMessage(message => println("UK: " + message));

    System.in.read()
  }
end TestClient
