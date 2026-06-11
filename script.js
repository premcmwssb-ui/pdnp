const link = document.getElementById("locationLink");
const statusEl = document.getElementById("status");
const result = document.getElementById("result");

link.addEventListener("click", (event) => {
  event.preventDefault();

  if (!("geolocation" in navigator)) {
    statusEl.textContent = "Geolocation is not supported by this browser.";
    return;
  }

  statusEl.textContent = "Requesting your location… please allow the permission prompt.";

  navigator.geolocation.getCurrentPosition(onSuccess, onError, {
    enableHighAccuracy: true,
    timeout: 10000,
    maximumAge: 0,
  });
});

function onSuccess(position) {
  const { latitude, longitude, accuracy } = position.coords;

  document.getElementById("lat").textContent = latitude.toFixed(6);
  document.getElementById("lng").textContent = longitude.toFixed(6);
  document.getElementById("acc").textContent = `±${Math.round(accuracy)} m`;

  const mapLink = document.getElementById("mapLink");
  mapLink.href = `https://www.google.com/maps?q=${latitude},${longitude}`;

  statusEl.textContent = "Location retrieved successfully.";
  result.classList.remove("hidden");
}

function onError(error) {
  result.classList.add("hidden");

  switch (error.code) {
    case error.PERMISSION_DENIED:
      statusEl.textContent = "Permission denied. Location access was not granted.";
      break;
    case error.POSITION_UNAVAILABLE:
      statusEl.textContent = "Location information is unavailable.";
      break;
    case error.TIMEOUT:
      statusEl.textContent = "The request to get your location timed out.";
      break;
    default:
      statusEl.textContent = "An unknown error occurred.";
  }
}
