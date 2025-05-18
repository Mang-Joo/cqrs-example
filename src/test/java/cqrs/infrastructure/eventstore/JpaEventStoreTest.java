package cqrs.infrastructure.eventstore;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import cqrs.bankaccount.model.BankAccount;
import cqrs.bankaccount.model.event.AccountCreatedEvent;
import cqrs.bankaccount.model.event.MoneyDepositedEvent;
import cqrs.common.Event;

@SpringBootTest
class JpaEventStoreTest {

    @Autowired
    private JpaEventStore jpaEventStore;

    @Test
    void testLoad() {
        UUID aggregateId = UUID.randomUUID();
        jpaEventStore.save(aggregateId, new AccountCreatedEvent(UUID.randomUUID(), aggregateId, "1234567890", "John Doe", LocalDateTime.now(), 0));
        jpaEventStore.save(aggregateId, new MoneyDepositedEvent(UUID.randomUUID(), aggregateId, BigDecimal.valueOf(100), LocalDateTime.now(), 1));
        List<Event> events = jpaEventStore.load(aggregateId);

        BankAccount account = BankAccount.load(aggregateId, events);


        assertThat(account.getAccountNumber()).isEqualTo("1234567890");
        assertThat(account.getAccountHolder()).isEqualTo("John Doe");
        assertThat(account.getBalance()).isEqualTo(BigDecimal.valueOf(100));
        assertThat(account.getNextVersion()).isEqualTo(events.size() + 1);

        assertThat(events).hasSize(2);
        assertThat(events.get(0)).isInstanceOf(AccountCreatedEvent.class);
        assertThat(events.get(1)).isInstanceOf(MoneyDepositedEvent.class);
    }
}