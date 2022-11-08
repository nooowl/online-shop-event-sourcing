package shopDemo

import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Criteria
import org.springframework.data.mongodb.core.query.Query
import ru.quipy.core.EventSourcingService
import shopDemo.api.OrderAggregate
import shopDemo.logic.Order
import shopDemo.logic.OrderState
import shopDemo.logic.PaymentMethod
import java.util.*

@SpringBootTest
class OrderEventStreamsTest {
    companion object {
        private val testId = UUID.randomUUID()
        private val userId = UUID.randomUUID()
    }

    @Autowired
    private lateinit var orderESService: EventSourcingService<UUID, OrderAggregate, Order>

    @Autowired
    lateinit var mongoTemplate: MongoTemplate

    @BeforeEach
    fun init() {
        cleanDatabase()
    }

    fun cleanDatabase() {
        mongoTemplate.remove(Query.query(Criteria.where("aggregateId").`is`(testId)), "orders")
        mongoTemplate.remove(Query.query(Criteria.where("_id").`is`(testId)), "snapshots")
    }

    @Test
    fun fullOrderTest() {
        val products = mapOf(
            UUID.fromString("811b77bb-433e-4e44-912c-50d92172ce47") to 4,
            UUID.fromString("811b77bb-433e-4e44-912c-50d92172ce48") to 1,
            UUID.fromString("811b77bb-433e-4e44-912c-50d92172ce49") to 2
        )
        orderESService.create {
            it.createNewOrder(testId, userId, products)
        }

        orderESService.update(testId) {
            it.addDeliveryAddress("City, Street, 12, 345")
        }
        orderESService.update(testId) {
            it.addDeliveryDate(Date())
        }
        orderESService.update(testId) {
            it.addPaymentMethod(PaymentMethod.Card)
        }

        orderESService.update(testId) {
            it.changeOrderState(OrderState.Processed)
        }

        orderESService.update(testId) {
            it.changeOrderState(OrderState.Paid)
        }

        val state = orderESService.getState(testId)!!
        println(state)
    }
}