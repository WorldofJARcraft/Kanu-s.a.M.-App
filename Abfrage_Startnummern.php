<?php
//Quelle: http://stefan-draeger-software.de/blog/android-app-mit-mysql-datenbank-verbinden/
//Datenbankverbindung aufbauen
$connection = mysqli_connect("localhost","root","","android_connect");
if (mysqli_connect_errno()) {
    echo mysql_errno($connection) . ": " . mysql_error($connection). "\n";
    die();
}
getMessstationencount($connection);
//getAllPlants($connection);

//Liefert alle deutschen Pflanzennamen zurück.
function getAllPlants($connection){
  $sqlStmt = "INSERT INTO `android_connect`.`messstation_".$_GET["name"]."` (`Startnummer`, `Zeitpunkt`) VALUES ('".$_GET["nummer"]."', CURRENT_TIMESTAMP);";
  $result =  mysqli_query($connection,$sqlStmt);
  $data = array();
  if ($result = $connection->query($sqlStmt)) {
      while ($row = $result->fetch_assoc()) {
        $id = $row["id"];
        $de_name = $row["de_name"];
        array_push($data,array("ID"=> $id,"de_name"=>$de_name));  
      }
      
  // Das Objekt wieder freigeben.
  
}

$result->free();
  closeConnection($connection);
  
   //Das Array durchlaufen und nur die deutschen Namen ausgeben.
  foreach ($data as $d){
    echo $d["de_name"];
    echo "|";
  }
  
  
}
function getMessstationencount ($connection) {
	$sqlStmt = "SELECT Startnummer FROM `namen` WHERE 1 ORDER BY `namen`.`Startnummer` ASC";
  $result =  mysqli_query($connection,$sqlStmt);
  if ($result = $connection->query($sqlStmt)) {
  
	while ($row = $result->fetch_assoc()) {
        $id = $row["Startnummer"];
        echo $id."|";  
      }
$result->free();
  closeConnection($connection);
}
}
//Verbindung schließen.
function closeConnection($connection){
  mysqli_close($connection);
}
?>