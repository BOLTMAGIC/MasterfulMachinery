# Recipe Entries

### Input Entries

```json
{
  "type": "mm:output/simple",
  "chance": 0.2, // Not required - Defaulted to 1
  "per_tick": true, // Nor Required - Defaulted to false
  "ingredient": {
    ...
  }
}
```

### Output Entries

```json
{
  "type": "mm:input/consume",
  "chance": 0.2, // Not required - Defaulted to 1
  "per_tick": true, // Nor Required - Defaulted to false
  "ingredient": {
    ...
  }
}
```
### Flex and count entries

| Type | Kind | |
|---|---|---|
| `mm:input/flex` | input | takes every kind of matching item in one run (recipe needs `"mode": "flex"`) |
| `mm:input/flex_match` | input | an extra item each flex kind needs, e.g. its crux |
| `mm:input/count` | input | counts matching items without consuming them |
| `mm:output/flex` | output | one output per flex kind, named from the input |

Outputs also take `"scale_by": "<as>"` to multiply their amount by a count or by the number of flex kinds.
See [FlexRecipes](FlexRecipes.md).
