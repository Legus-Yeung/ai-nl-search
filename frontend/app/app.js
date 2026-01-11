var app = angular.module("nlSearchApp", []);

app.controller("SearchController", function ($scope, $http) {
  $scope.query = "";
  $scope.results = [];
  $scope.filters = null;
  $scope.loading = false;
  $scope.lastSearchQuery = "";
  $scope.error = null;
  $scope.warnings = [];
  $scope.followUp = null;

  $scope.search = function () {
    if (!$scope.query || !$scope.query.trim()) {
      $scope.error = "Please enter a search query";
      return;
    }

    $scope.loading = true;
    $scope.error = null;
    $scope.warnings = [];
    $scope.followUp = null;
    var searchQuery = $scope.query.trim();
    $scope.lastSearchQuery = searchQuery;

    $http.post("/api/nl-search", {
      query: searchQuery
    }).then(function (response) {
      $scope.results = response.data.results || [];
      $scope.filters = response.data.filters;
      $scope.warnings = response.data.warnings || [];
      $scope.followUp = response.data.followUp || null;
      $scope.query = "";
    }).catch(function (error) {
      var errorMessage = "Search failed";
      if (error.data && error.data.message) {
        errorMessage = error.data.message;
      } else if (error.statusText) {
        errorMessage = error.statusText;
      }
      $scope.error = errorMessage;
      console.error(error);
    }).finally(function () {
      $scope.loading = false;
    });
  };

  $scope.getFilterSummary = function(filters) {
    if (!filters) return [];
    var summary = [];
    
    if (filters.location_name) {
      summary.push({ label: "Location", value: filters.location_name });
    }
    if (filters.location_type) {
      summary.push({ label: "Location Type", value: filters.location_type });
    }
    if (filters.city) {
      summary.push({ label: "City", value: filters.city });
    }
    if (filters.company_name) {
      summary.push({ label: "Company", value: filters.company_name });
    }
    if (filters.carrier_name) {
      summary.push({ label: "Carrier", value: filters.carrier_name });
    }
    if (filters.service && filters.service.length > 0) {
      summary.push({ label: "Service", value: filters.service.join(", ") });
    }
    if (filters.collected_by && filters.collected_by.length > 0) {
      summary.push({ label: "Collected By", value: filters.collected_by.join(", ") });
    }
    if (filters.flags && filters.flags.length > 0) {
      summary.push({ label: "Flags", value: filters.flags.join(", ") });
    }
    
    var dateField = (filters.date_field || "CREATED").charAt(0) + (filters.date_field || "CREATED").slice(1).toLowerCase();
    if (filters.date_from || filters.date_to) {
      var dateRange = "";
      if (filters.date_from && filters.date_to) {
        dateRange = filters.date_from + " → " + filters.date_to;
      } else if (filters.date_from) {
        dateRange = "From " + filters.date_from;
      } else if (filters.date_to) {
        dateRange = "Until " + filters.date_to;
      }
      summary.push({ label: "Date (" + dateField + ")", value: dateRange });
    }
    
    var exclusions = [];
    if (filters.exclude_status && filters.exclude_status.length > 0) {
      exclusions.push("Status: " + filters.exclude_status.join(", "));
    }
    if (filters.exclude_location_name) {
      exclusions.push("Location: " + filters.exclude_location_name);
    }
    if (filters.exclude_location_type && filters.exclude_location_type.length > 0) {
      exclusions.push("Location Type: " + filters.exclude_location_type.join(", "));
    }
    if (filters.exclude_city) {
      exclusions.push("City: " + filters.exclude_city);
    }
    if (filters.exclude_company_name) {
      exclusions.push("Company: " + filters.exclude_company_name);
    }
    if (filters.exclude_carrier_name) {
      exclusions.push("Carrier: " + filters.exclude_carrier_name);
    }
    if (filters.exclude_service && filters.exclude_service.length > 0) {
      exclusions.push("Service: " + filters.exclude_service.join(", "));
    }
    if (filters.exclude_collected_by && filters.exclude_collected_by.length > 0) {
      exclusions.push("Collected By: " + filters.exclude_collected_by.join(", "));
    }
    if (filters.exclude_flags && filters.exclude_flags.length > 0) {
      exclusions.push("Flags: " + filters.exclude_flags.join(", "));
    }
    
    if (exclusions.length > 0) {
      summary.push({ label: "Excluded", value: exclusions.join("; ") });
    }
    
    return summary;
  };
});
