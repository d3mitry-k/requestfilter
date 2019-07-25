# requestfilter


// Написать реализацию Map (расширить HashMap), которая в конструкторе бы
// принимала на вход класс Enum и заполняла бы значения исходя из следующего
// правила:
// - Ключами для Map являются значения Enum
// - Значениями для Map являются строки
// - Значения строк вычитываются из Enum используя метод, имеющий аннотацию @JsonValue
// - Метод, аннотированный @JsonValue, должен быть публичным, иметь ноль аргументов
//   и возвращать строку. Это всё требуется проверить
// - Полученный объект должен быть Generic
// - Дополнительно: нужно создать unit test
//
// Пример Enum, который можно передать в такой класс:
// public  enum Gender {
//     MALE, FEMALE;
//
//     @JsonValue
//     public String toValue() {
//         return name().toLowerCase();
//     }
// }
// При этом Map должен иметь значения:
// [Gender.MALE] = "male"
// [Gender.FEMALE] = "female