import com.muqiuhan.hanmq.client.{Producer, Consumer}

import java.net.URI

class TestClient extends munit.FunSuite:
  val url = "ws://localhost:9993/";

  test("Test client") {
    val producer1 = new Producer(URI.create(url), "producer_1");
    val producer2 = new Producer(URI.create(url), "producer_2");
    val producer3 = new Producer(URI.create(url), "producer_3");
    val consumer1 = new Consumer(URI.create(url), "American");
    val consumer2 = new Consumer(URI.create(url), "China");
    val consumer3 = new Consumer(URI.create(url), "UK");

    producer1.send("Make America Great Again!", "American.great.again.!");
    producer2.send("China is getting stronger!", "China.daily.com");
    producer2.send("China sees 14.3 percent more domestic trips in H1", "China.xinhua.net");
    producer3.send("The voice from Europe", "UK.Reuters.com");

    consumer1.register("American", true);
    consumer1.onMessage(message => scribe.info("American: " + message));
    consumer2.register("China", true);
    consumer2.onMessage(message => scribe.info("China: " + message));
    consumer3.register("UK", true);
    consumer3.onMessage(message => scribe.info("UK: " + message));
  }
end TestClient
