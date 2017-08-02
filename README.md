# Pricing Service

This is the pricing service. It subscribes to messages from the `sessions` exchange, 
adds a price, and puts them on the `transactions` exchange. 

## Run

* Make sure RabbitMQ is running and both exchanges are created. This can be done with 
the shared-dev-env repo. 
* Run `lein run`

## API

An api is available with the following endpoints:

* GET `/policies/:id` get policy with the given ID
* POST `/policies` create new policy with given `name` (as JSON body)
* POST `/policies/:id/elements` create new policy element with given `start_at`, 
`end_at`, `time_from`, `time_to`, `days`, `unit`, `step_size` and `price`

## RabbitMQ testing

Put this JSON in the `session` queue to test. It should end up in the transaction service. 

```
{
  "evse_id": "790838973-00002",
  "started_at": "2016-01-30T17:39:12Z",
  "ended_at": "2016-01-30T18:39:12Z",
  "volume": 17.453634968809,
  "user_id": "John"
}
```