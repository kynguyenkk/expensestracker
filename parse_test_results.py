#!/usr/bin/env python3
"""
Parse Real Test Results and Generate Excel Report
Parses Surefire XML, manual test logs, and penetration test logs
Outputs: security_test_results.csv with REAL data
"""

import xml.etree.ElementTree as ET
import csv
import os
import re
from datetime import datetime
from pathlib import Path

class TestResultsParser:
    def __init__(self):
        self.results = []
        self.test_counter = 1
        self.timestamp = datetime.now().strftime("%Y-%m-%d %H:%M:%S")
        
    def parse_surefire_xml(self, surefire_dir="target/surefire-reports"):
        """Parse Surefire XML reports for automated test results"""
        print(f"📋 Parsing Surefire XML reports from {surefire_dir}...")
        
        if not os.path.exists(surefire_dir):
            print(f"   ⚠ Warning: {surefire_dir} not found. Run 'mvn test' first.")
            return
        
        xml_files = list(Path(surefire_dir).glob("TEST-*.xml"))
        
        if not xml_files:
            print(f"   ⚠ Warning: No XML files found in {surefire_dir}")
            return
        
        for xml_file in xml_files:
            try:
                tree = ET.parse(xml_file)
                root = tree.getroot()
                
                # Get test suite info
                suite_name = root.get('name', '').split('.')[-1]  # Get class name only
                
                # Categorize based on class name
                if 'Encryption' in suite_name:
                    category = 'Encryption'
                elif 'Jwt' in suite_name or 'JWT' in suite_name:
                    category = 'JWT'
                elif 'Otp' in suite_name or 'OTP' in suite_name:
                    category = 'OTP'
                elif 'BruteForce' in suite_name:
                    category = 'Brute Force'
                elif 'Integration' in suite_name or 'Journey' in suite_name:
                    category = 'Integration'
                else:
                    category = 'Automated'
                
                # Parse each test case
                for testcase in root.findall('testcase'):
                    test_name = testcase.get('name', 'Unknown')
                    time = testcase.get('time', '0.0')
                    
                    # Check for failure/error
                    failure = testcase.find('failure')
                    error = testcase.find('error')
                    skipped = testcase.find('skipped')
                    
                    if failure is not None:
                        status = 'FAIL'
                        actual_result = failure.get('message', 'Test failed')[:200]
                        actual_result = actual_result.replace(',', ';').replace('\n', ' ')
                    elif error is not None:
                        status = 'ERROR'
                        actual_result = error.get('message', 'Test error')[:200]
                        actual_result = actual_result.replace(',', ';').replace('\n', ' ')
                    elif skipped is not None:
                        status = 'SKIP'
                        actual_result = 'Test skipped'
                    else:
                        status = 'PASS'
                        actual_result = 'Test passed successfully'
                    
                    # Generate description from test name
                    description = test_name.replace('test', '').replace('_', ' ').strip()
                    description = re.sub(r'([A-Z])', r' \1', description).strip()
                    
                    self.results.append({
                        'test_id': f'TC-{self.test_counter:03d}',
                        'category': category,
                        'test_name': test_name,
                        'description': f'Verify {description}',
                        'actual_result': actual_result,
                        'status': status,
                        'execution_time': f'{time}s',
                        'timestamp': self.timestamp
                    })
                    
                    self.test_counter += 1
                    
            except Exception as e:
                print(f"   ⚠ Error parsing {xml_file}: {e}")
        
        print(f"   ✓ Parsed {self.test_counter - 1} automated tests from Surefire reports")
    
    def parse_manual_test_log(self, log_file="manual_test_results.log"):
        """Parse manual security test log output"""
        print(f"📋 Parsing manual test results from {log_file}...")
        
        if not os.path.exists(log_file):
            print(f"   ⚠ Warning: {log_file} not found.")
            print(f"   Run: bash scripts/manual_security_tests.sh > {log_file}")
            return
        
        with open(log_file, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # Parse PASS results
        pass_matches = re.findall(r'✓ PASS: (.+)', content)
        for match in pass_matches:
            self.results.append({
                'test_id': f'TC-{self.test_counter:03d}',
                'category': 'Manual',
                'test_name': match[:50],
                'description': f'Manual verification: {match[:100]}',
                'actual_result': match[:200],
                'status': 'PASS',
                'execution_time': 'N/A',
                'timestamp': self.timestamp
            })
            self.test_counter += 1
        
        # Parse FAIL results
        fail_matches = re.findall(r'✗ FAIL: (.+)', content)
        for match in fail_matches:
            self.results.append({
                'test_id': f'TC-{self.test_counter:03d}',
                'category': 'Manual',
                'test_name': match[:50],
                'description': f'Manual verification: {match[:100]}',
                'actual_result': match[:200],
                'status': 'FAIL',
                'execution_time': 'N/A',
                'timestamp': self.timestamp
            })
            self.test_counter += 1
        
        manual_count = len(pass_matches) + len(fail_matches)
        print(f"   ✓ Parsed {manual_count} manual tests ({len(pass_matches)} PASS, {len(fail_matches)} FAIL)")
    
    def parse_penetration_test_log(self, log_file="penetration_test_results.log"):
        """Parse penetration test log output"""
        print(f"📋 Parsing penetration test results from {log_file}...")
        
        if not os.path.exists(log_file):
            print(f"   ⚠ Warning: {log_file} not found.")
            print(f"   Run: bash scripts/penetration_tests.sh all > {log_file}")
            return
        
        with open(log_file, 'r', encoding='utf-8', errors='ignore') as f:
            content = f.read()
        
        # Parse BLOCKED results
        blocked_matches = re.findall(r'✓ BLOCKED: (.+)', content)
        for match in blocked_matches:
            self.results.append({
                'test_id': f'TC-{self.test_counter:03d}',
                'category': 'Penetration',
                'test_name': match[:50],
                'description': f'Security test: {match[:100]}',
                'actual_result': f'Attack blocked: {match[:150]}',
                'status': 'BLOCKED',
                'execution_time': 'N/A',
                'timestamp': self.timestamp
            })
            self.test_counter += 1
        
        # Parse VULNERABLE results
        vuln_matches = re.findall(r'✗ VULNERABLE: (.+)', content)
        for match in vuln_matches:
            self.results.append({
                'test_id': f'TC-{self.test_counter:03d}',
                'category': 'Penetration',
                'test_name': match[:50],
                'description': f'Security test: {match[:100]}',
                'actual_result': f'Vulnerability found: {match[:150]}',
                'status': 'VULNERABLE',
                'execution_time': 'N/A',
                'timestamp': self.timestamp
            })
            self.test_counter += 1
        
        pen_count = len(blocked_matches) + len(vuln_matches)
        print(f"   ✓ Parsed {pen_count} penetration tests ({len(blocked_matches)} BLOCKED, {len(vuln_matches)} VULNERABLE)")
    
    def generate_csv(self, output_file="security_test_results.csv"):
        """Generate CSV file with all test results"""
        print(f"\n📊 Generating CSV report: {output_file}...")
        
        if not self.results:
            print("   ⚠ No test results found!")
            return
        
        with open(output_file, 'w', newline='', encoding='utf-8') as csvfile:
            fieldnames = ['Test ID', 'Category', 'Test Name', 'Description', 
                         'Actual Results', 'Status', 'Execution Time', 'Timestamp']
            writer = csv.DictWriter(csvfile, fieldnames=fieldnames)
            
            writer.writeheader()
            
            for result in self.results:
                writer.writerow({
                    'Test ID': result['test_id'],
                    'Category': result['category'],
                    'Test Name': result['test_name'],
                    'Description': result['description'],
                    'Actual Results': result['actual_result'],
                    'Status': result['status'],
                    'Execution Time': result['execution_time'],
                    'Timestamp': result['timestamp']
                })
        
        # Statistics
        total = len(self.results)
        passed = sum(1 for r in self.results if r['status'] == 'PASS')
        failed = sum(1 for r in self.results if r['status'] == 'FAIL')
        blocked = sum(1 for r in self.results if r['status'] == 'BLOCKED')
        errors = sum(1 for r in self.results if r['status'] == 'ERROR')
        
        print(f"\n{'='*65}")
        print(f"   Security Test Report Generated Successfully!")
        print(f"{'='*65}")
        print(f"\n   📊 Output: {output_file}")
        print(f"\n   📈 Statistics:")
        print(f"      Total Tests:       {total}")
        print(f"      ✓ Passed:          {passed}")
        print(f"      ✗ Failed:          {failed}")
        print(f"      ⚠ Errors:          {errors}")
        print(f"      🛡 Blocked:         {blocked}")
        print(f"\n   📁 Location: {os.path.abspath(output_file)}")
        print(f"\n   ✅ Ready to open in Excel!")
        print()

def main():
    print("\n╔══════════════════════════════════════════════════════════════╗")
    print("║    Security Test Results Parser - Real Data Extraction      ║")
    print("╚══════════════════════════════════════════════════════════════╝\n")
    
    parser = TestResultsParser()
    
    # Parse all sources
    parser.parse_surefire_xml()
    parser.parse_manual_test_log()
    parser.parse_penetration_test_log()
    
    # Generate CSV
    parser.generate_csv()

if __name__ == "__main__":
    main()
