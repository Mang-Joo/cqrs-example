package cqrs.infrastructure.eventstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cqrs.bankaccount.model.BankAccount;
import cqrs.bankaccount.model.event.CreateAccountEvent;
import cqrs.bankaccount.model.event.DepositEvent;
import cqrs.common.Event;

@SpringBootTest
class JpaEventStoreTest {

    @Autowired
    private JpaEventStore jpaEventStore;

    @Test
    void testLoad() {
        UUID aggregateId = UUID.randomUUID();
        jpaEventStore.save(aggregateId, new CreateAccountEvent("1234567890", "John Doe", 0));
        jpaEventStore.save(aggregateId, new DepositEvent(BigDecimal.valueOf(100), 1));
        List<Event> events = jpaEventStore.load(aggregateId);
        BankAccount account = new BankAccount(aggregateId, events);


        assertThat(account.getAccountNumber()).isEqualTo("1234567890");
        assertThat(account.getAccountHolder()).isEqualTo("John Doe");
        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(100));

        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(CreateAccountEvent.class);
        assertThat(events.get(1)).isInstanceOf(DepositEvent.class);
    }
}