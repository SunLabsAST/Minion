#!/usr/bin/perl

use FileHandle;
use File::Basename;


$inBody = 0;
while (<>) {
  if (/^<!--X-([^:]*): (.*) -->/) {
    $hname = $1;
    $header = $2;
    if($hname eq "Subject" ||
       $hname eq "From") {
      print "$hname $header\n";
    }
    next;
  } elsif (/<!--X-Body-of-Message-->/) {
    $inBody = 1;
    next;
  } elsif (/<!--X-Body-of-Message-End-->/) {
    $inBody = 0;
    next;
  }
  if ($inBody) {
    print $_;
  }
}
