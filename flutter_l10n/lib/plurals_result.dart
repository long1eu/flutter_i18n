// File created by
// Lung Razvan <long1eu>
// on 2019-06-20

part of 'flutter_module.dart';

class PluralsResult {
  const PluralsResult(this.pluralsIds, this.pluralsQuantities);

  final List<String> pluralsIds;
  final Map<String, List<String>> pluralsQuantities;

  @override
  bool operator ==(Object other) =>
      identical(this, other) ||
      other is PluralsResult &&
          runtimeType == other.runtimeType &&
          const ListEquality<String>().equals(pluralsIds, other.pluralsIds) &&
          const MapEquality<String, List<String>>(values: ListEquality<String>())
              .equals(pluralsQuantities, other.pluralsQuantities);

  @override
  int get hashCode =>
      const ListEquality<String>().hash(pluralsIds) ^
      const MapEquality<String, List<String>>(values: ListEquality<String>()).hash(pluralsQuantities);

  @override
  String toString() => 'PluralsResult{pluralIds: $pluralsIds, pluralQuantities: $pluralsQuantities}';
}
